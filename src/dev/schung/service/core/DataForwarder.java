package dev.schung.service.core;

import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BLE 感測資料轉發器
 *
 * @author schung
 */
public class DataForwarder {

    final static Logger logger = Logger.getLogger(DataForwarder.class.getName());

    public final static int DATA_BUFFER_SIZE = 512;
    private final static int WRITE_TIMEOUT = 1000;

    private final String nic; // 網路介面
    private final String host; // 接收端 IP
    private final int port; // 接收端 Port

    private DatagramSocket dgSkt;

    /**
     *
     * @param nic 網卡
     * @param host 接收端 IP
     * @param port UDP 監聽埠
     */
    public DataForwarder(String nic, String host, int port) {
        this.nic = nic;
        this.host = host;
        this.port = port;
    }

    public DataForwarder(String host, int port) {
        this("lo", host, port);
    }


    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    public void init() throws SocketException, UnknownHostException {
        InetAddress address = null;
        if (host.equalsIgnoreCase("127.0.0.1")) {
            NetworkInterface nif = NetworkInterface.getByName(nic);
            if (nif != null) {
                Enumeration<InetAddress> addresses = nif.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    address = addresses.nextElement();
                    if (address.isLoopbackAddress()) {
                        address = addresses.nextElement();
                        logger.log(Level.INFO, String.format("Bind to interface %s, ip %s", nif.getDisplayName(), address));
                        break;
                    }
                }
            }
        } else {
            address = InetAddress.getByName(host);
        }
        //
        if (address != null) {
            dgSkt = new DatagramSocket();
            dgSkt.connect(new InetSocketAddress(address, port));
            dgSkt.setReuseAddress(true);
            dgSkt.setSoTimeout(WRITE_TIMEOUT);
            dgSkt.setSendBufferSize(DATA_BUFFER_SIZE);
        }
    }

    public void release() {
        if (dgSkt != null) {
            dgSkt.close();
        }
        dgSkt = null;
        logger.log(Level.INFO, "釋放 DataForwarder instance");
    }

    public synchronized void sendEvent(JSONObject eventObject) throws IOException {
        logger.log(Level.INFO, String.format("Send UDP Data\r\n%s", eventObject.toString()));
        if (dgSkt == null) {
            init();
        }
        try {
            //logger.log(Level.INFO, String.format("Send UDP Data\r\n%s", eventObject.toString()));
            byte[] data = eventObject.toString().getBytes();
            DatagramPacket dpkt = new DatagramPacket(data, data.length);
            dgSkt.send(dpkt);
            logger.log(Level.INFO, String.format("Send UDP Data %d bytes to %s:%d", data.length, host, port));
        } catch (IOException ex) {
            logger.log(Level.WARNING, String.format("Send UDP Data Error: %s", ex.toString()));
            try {
                dgSkt.close();
            } catch (Exception e) {
            }
            dgSkt = null;
        }
    }

}
