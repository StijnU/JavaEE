package session;

import java.util.Set;
import javax.ejb.Remote;
import rental.CarType;
import rental.Reservation;

@Remote
public interface ManagerSessionRemote {
    
    public Set<CarType> getCarTypes(String company);
    
    public Set<Integer> getCarIds(String company,String type);
    
    public int getNumberOfReservations(String company, String type, int carId);
    
    public int getNumberOfReservations(String company, String type);

    public int getNumberOfReservationsForOneClient(String clientName);

    public Set<String> getBestClients();

    public CarType getMostPopularCarTypeIn(String carRentalCompanyName, int year);
    
    public void loadRental(String datafile);
      
}