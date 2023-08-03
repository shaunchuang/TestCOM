package VisCOM;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListenerWithExceptions;
import dev.schung.service.core.DataForwarder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    static DataForwarder dataForwarder = new DataForwarder("lo","127.0.0.1",4444);

    public static void main(String[] args) throws IOException {
        String desc = "USB-Serial Controller D";
        SerialPort target = null;
        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                System.out.println(port.getPortDescription());
                if (port.getDescriptivePortName().contains("USB")) {
                    target = port;

                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (target != null) {
            target.setBaudRate(2400);
            target.setNumDataBits(8);
            target.setNumStopBits(1);
            boolean opened = target.openPort();
            System.out.println("Open COM " + target.getSystemPortName() + " " + opened);

            JSONObject jsonObject = new JSONObject();
            try {
                dataForwarder.init();
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            if (target.isOpen()) {

                target.addDataListener(new SerialPortMessageListenerWithExceptions() {
                    @Override
                    public void catchException(Exception ex) {
                        ex.printStackTrace();
                    }

                    @Override
                    public byte[] getMessageDelimiter() {
                        return new byte[]{0x0D};
                    }

                    @Override
                    public boolean delimiterIndicatesEndOfMessage() {
                        return true;
                    }

                    @Override
                    public int getListeningEvents() {
                        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
                    }

                    @Override
                    public void serialEvent(SerialPortEvent event) {

                        String s_raw = new String(event.getReceivedData(), StandardCharsets.UTF_8);
                        System.out.println(s_raw);
                        String s = s_raw.trim();
                        System.out.println(s);
                        String regex = "R(\\d+\\.\\d+)L(\\d+\\.\\d+)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.find()){
                            String rightEye = matcher.group(1);
                            String leftEye = matcher.group(2);
                            double rightEyeD = Double.parseDouble(rightEye);
                            double leftEyeD = Double.parseDouble(leftEye);

                            JSONObject eventObject = new JSONObject();
                            eventObject.put("event", "DATA_UPDATE");
                            eventObject.put("alias", "VIS");
                            eventObject.put("macAddress", "");
                            eventObject.put("dataType", "VIS");
                            JSONArray value_array = new JSONArray();
                            value_array.put(rightEyeD);
                            value_array.put(leftEyeD);
                            eventObject.put("value", value_array);

                            System.out.println(value_array);
                            try {
                                dataForwarder.sendEvent(eventObject);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            
                        }
                    }
                });
                //
                while (target.isOpen()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (Exception ex) {
                    }
                }

            }
            target.closePort();
        }
    }

}