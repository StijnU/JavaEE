package session;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJBException;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Quote;
import rental.RentalStore;
import rental.Reservation;
import rental.ReservationConstraints;
import rental.ReservationException;
import sun.security.krb5.internal.crypto.crc32;

@Stateful
public class ReservationSession implements ReservationSessionRemote {
    
    @PersistenceContext
    EntityManager em;

    private String renter;
    private List<Quote> quotes = new LinkedList<Quote>();

    @Override
    public Set<String> getAllRentalCompanyNames() {
        return getRentals().keySet();
    }
    
    private Set<CarRentalCompany> getAllRentalCompanies() {
        return new HashSet<CarRentalCompany>(getRentals().values());
    }
    
    public CarRentalCompany getRentalCompany(String crc){
        System.out.print("getRentalComp: " + getRentals().get(crc).toString());
        return getRentals().get(crc);
    }
    
    @Override
    public List<CarType> getAvailableCarTypes(Date start, Date end) {
        List<CarType> availableCarTypes = new LinkedList<CarType>();
        for(String crc : getAllRentalCompanyNames()) {
            for(CarType ct : getRentalCompany(crc).getAvailableCarTypes(start, end)) {
                if(!availableCarTypes.contains(ct))
                    availableCarTypes.add(ct);
            }
        }
        return availableCarTypes;
    }

    @Override
    public Quote createQuote(String renter, ReservationConstraints constraints) throws ReservationException {
        Quote out = null;
        Set<CarRentalCompany> carRentalCompanies = getAllRentalCompanies();
        for(CarRentalCompany crc : carRentalCompanies){
                try {
                out = crc.createQuote(constraints, renter);
                System.out.print("Quote made: " + out.toString());
                quotes.add(out);
               }
        catch(Exception e) {
            System.out.print("ERROR: " + e.getMessage());
        }}
         if(out == null){
             throw new ReservationException("No quotes available");
         }
         else{
             return out;
         }
    }

    @Override
    public List<Quote> getCurrentQuotes() {
        return quotes;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Reservation> confirmQuotes() throws ReservationException {
        List<Reservation> done = new LinkedList<Reservation>();
        
        try {
            for (Quote quote : quotes) {
                done.add(getRentalCompany(quote.getRentalCompany()).confirmQuote(quote));
            }
        } catch (Exception ex) {
            throw new EJBException ("Transaction failed: " + ex.getMessage());
        }
        quotes.clear();
        return done;
    }

    @Override
    public void setRenterName(String name) {
        if (renter != null) {
            throw new IllegalStateException("name already set");
        }
        renter = name;
    }

    @Override
    public String getRenterName() {
        return renter;
    }

    @Override
    public String getCheapestCarType(Date start, Date end, String region) {
        List<CarType> availableCarTypes = new LinkedList<CarType>();
        for(String crc : getAllRentalCompanyNames()) {
            CarRentalCompany carRentalCompany = getRentals().get(crc);
            if(carRentalCompany.getRegions().contains(region) || carRentalCompany.getRegions() == null){
                for(CarType ct : carRentalCompany.getAvailableCarTypes(start, end)) {
                    if(!availableCarTypes.contains(ct))
                        availableCarTypes.add(ct);}
            }
        }
        if(availableCarTypes.isEmpty()){
            return null;
        }
        CarType cheapestCarType = availableCarTypes.get(0);
        double price = availableCarTypes.get(0).getRentalPricePerDay();
        int i;
        for(i=1;i < availableCarTypes.size();i++){
            if(availableCarTypes.get(i).getRentalPricePerDay() <= price){
                price = availableCarTypes.get(i).getRentalPricePerDay();
                cheapestCarType = availableCarTypes.get(i);
            } 
        }
        return cheapestCarType.getName();     
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