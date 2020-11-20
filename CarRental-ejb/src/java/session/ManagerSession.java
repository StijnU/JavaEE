package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.Car;
import rental.CarRentalCompany;
import rental.CarType;
import rental.RentalStore;
import rental.Reservation;

@Stateless
public class ManagerSession implements ManagerSessionRemote {
    
    @PersistenceContext
    EntityManager em;
    
    @Override
    public Set<CarType> getCarTypes(String company) {
        try {
            return new HashSet<CarType>(getRentalCompany(company).getAllTypes());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public Set<Integer> getCarIds(String company, String type) {
        Set<Integer> out = new HashSet<Integer>();
        try {
            for(Car c: getRentalCompany(company).getCars(type)){
                out.add(c.getId());
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return out;
    }

    @Override
    public int getNumberOfReservations(String company, String type, int id) {
        try {
            return getRentalCompany(company).getCar(id).getReservations().size();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public int getNumberOfReservations(String company, String type) {
        Set<Reservation> out = new HashSet<Reservation>();
        try {
            for(Car c: getRentalCompany(company).getCars(type)){
                out.addAll(c.getReservations());
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
        return out.size();
    }
    
    @Override
    public int getNumberOfReservationsForOneClient(String renter){
        int numberOfReservations = 0;
        Set<String> allCarRentalCompanies = getAllRentalCompanies();
        for(String carRentalCompanyName : allCarRentalCompanies){
            CarRentalCompany carRentalCompany = getRentalCompany(carRentalCompanyName);
            numberOfReservations += carRentalCompany.getReservationsBy(renter).size();
        }
        return numberOfReservations;
    }
    
    private Set<String> getAllRentalCompanies() {
        return getRentals().keySet();
    }
    
    private CarRentalCompany getRentalCompany(String crc){
        return getRentals().get(crc);
    }

    @Override
    public Set<String> getBestClients() {
        Set<String> allRentalCompanies = getAllRentalCompanies();
        Set<Reservation> reservations = new HashSet<>();
        for(String crc : allRentalCompanies){
            reservations.addAll(getRentalCompany(crc).getReservations());
        }
        List<String> allClients = new LinkedList<>();
        for(Reservation reservation : reservations){
            if(!allClients.contains(reservation.getCarRenter()))
                    allClients.add(reservation.getCarRenter());
        }
        
        Set<String> bestClients = new HashSet<>();
        if(allClients.isEmpty()){
            return bestClients;
        }
        bestClients.add(allClients.get(0));
        int maxNbOfReservations = getNumberOfReservationsForOneClient(allClients.get(0));
        int i;
        for(i=1;i < allClients.size();i++){
            if(getNumberOfReservationsForOneClient(allClients.get(i)) > maxNbOfReservations){
                bestClients.clear();
                bestClients.add(allClients.get(i));
            } 
            else if (getNumberOfReservationsForOneClient(allClients.get(i)) == maxNbOfReservations){
                bestClients.add(allClients.get(i));
            }
        }
        return bestClients;
    }

    @Override
    public CarType getMostPopularCarTypeIn(String carRentalCompanyName, int year) {
        CarRentalCompany carRentalCompany = getRentalCompany(carRentalCompanyName);
        Set<Reservation> allReservations = carRentalCompany.getReservations();
        Map<CarType, Integer> nbOfReservationsByCarType = new HashMap<>();
        for (CarType carType : carRentalCompany.getAllTypes()) {
	        nbOfReservationsByCarType.put(carType, 0);
	    }
        for(Reservation reservation : allReservations){
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
				cal.setTime(reservation.getStartDate());
				if(cal.get(Calendar.YEAR) == year) {
                                    CarType carType = carRentalCompany.getType(reservation.getCarType());
                                    nbOfReservationsByCarType.put(carType,
                                            nbOfReservationsByCarType.get(carType) + 1);
                                }
        }
        int maxReservations = 0;
        CarType mostPopularCarType = null;
        for (Map.Entry<CarType, Integer> entry : nbOfReservationsByCarType.entrySet()) {
               if(mostPopularCarType == null || entry.getValue() >= maxReservations){
                   mostPopularCarType = entry.getKey();
                   maxReservations = entry.getValue();
               }
            }
        return mostPopularCarType;
    }
    
    
    
    @Override
    public void loadRental(String datafile) {
        try {
            CrcData data = loadData(datafile);
            CarRentalCompany company = new CarRentalCompany(data.name, data.regions, data.cars);
            em.persist(company);
            Logger.getLogger(RentalStore.class.getName()).log(Level.INFO, "Loaded {0} from file {1}", new Object[]{data.name, datafile});
        } catch (NumberFormatException ex) {
            Logger.getLogger(RentalStore.class.getName()).log(Level.SEVERE, "bad file", ex);
        } catch (IOException ex) {
            Logger.getLogger(RentalStore.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public CrcData loadData(String datafile)
            throws NumberFormatException, IOException {

        CrcData out = new CrcData();
        StringTokenizer csvReader;
        int nextuid = 0;
       
        //open file from jar
        BufferedReader in = new BufferedReader(new InputStreamReader(RentalStore.class.getClassLoader().getResourceAsStream(datafile)));
        
        try {
            while (in.ready()) {
                String line = in.readLine();
                
                if (line.startsWith("#")) {
                    // comment -> skip					
                } else if (line.startsWith("-")) {
                    csvReader = new StringTokenizer(line.substring(1), ",");
                    out.name = csvReader.nextToken();
                    out.regions = Arrays.asList(csvReader.nextToken().split(":"));
                } else {
                    csvReader = new StringTokenizer(line, ",");
                    //create new car type from first 5 fields
                    CarType type = new CarType(csvReader.nextToken(),
                            Integer.parseInt(csvReader.nextToken()),
                            Float.parseFloat(csvReader.nextToken()),
                            Double.parseDouble(csvReader.nextToken()),
                            Boolean.parseBoolean(csvReader.nextToken()));
                    //create N new cars with given type, where N is the 5th field
                    for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
                        Car car = new Car(type);
                        out.cars.add(car);
                        
                    }        
                }
            } 
        } finally {
            in.close();
        }

        return out;
    }
    
    static class CrcData {
            public List<Car> cars = new LinkedList<Car>();
            public String name;
            public List<String> regions =  new LinkedList<String>();
    }
    
    public synchronized Map<String, CarRentalCompany> getRentals(){
       List<CarRentalCompany> results = em.createNamedQuery("CarRentalCompany.FindAll").getResultList();
       Map<String, CarRentalCompany> carRentalCompanies = new HashMap<>();
       for(CarRentalCompany crc : results){
           carRentalCompanies.put(crc.getName(), crc);
       }
       return carRentalCompanies;
   }

}