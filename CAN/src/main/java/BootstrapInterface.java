import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface Bootstrap Interface provides interface for the bootstrap server
 * 
 *
 *
 */
public interface BootstrapInterface extends Remote {

	/**
	 * set the IP Address
	 * 
	 * @param ip
	 * @throws RemoteException
	 */
	public void setIPAddress(String ip) throws RemoteException;

	/**
	 * get the IP Address
	 * 
	 * @return IP Address
	 * @throws RemoteException
	 */
	public String getIPAddress(String incoming) throws RemoteException;

	/**
	 * get random coordinate
	 * 
	 * @return Random coordinate
	 * @throws RemoteException
	 */
	public double getRandomCoordinate() throws RemoteException;

}
