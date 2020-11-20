package rental;

import javax.annotation.Generated;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({ 
    @NamedQuery(name="Reservation.FindAllReservationsOfClient", query="SELECT COUNT(*) FROM Reservation r WHERE r.carRenter = :carrenter"),
    @NamedQuery(name="Reservation.FindAllReservationsOfCarType", query="SELECT COUNT(*) FROM Reservation r WHERE r.carType = :cartype"),    
})
public class Reservation extends Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Column
    private int carId;
    
    /***************
     * CONSTRUCTOR *
     ***************/
    
    public Reservation(){}
    
    public Reservation(Quote quote, int carId) {
    	super(quote.getCarRenter(), quote.getStartDate(), quote.getEndDate(), 
    		quote.getRentalCompany(), quote.getCarType(), quote.getRentalPrice());
        this.carId = carId;
    }
    
    /******
     * ID *
     ******/
    
    public int getCarId() {
    	return carId;
    }
    
    /*************
     * TO STRING *
     *************/
    
    @Override
    public String toString() {
        return String.format("Reservation for %s from %s to %s at %s\nCar type: %s\tCar: %s\nTotal price: %.2f", 
                getCarRenter(), getStartDate(), getEndDate(), getRentalCompany(), getCarType(), getCarId(), getRentalPrice());
    }	
}