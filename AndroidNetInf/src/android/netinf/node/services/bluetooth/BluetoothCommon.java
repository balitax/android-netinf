package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Ndo;
import android.os.Environment;
import android.util.Log;

// In case you don't want to send length prefixes
// it might have been nice to close the OutputStream after writing the message
// At the time of writing that doesn't seem to work
// If the OutputStream is closed, an IOException (already closed)
// is thrown when trying to read from the InputStream
// Since the OutputStream isn't closed the parser on the server side gets stuck
// Instead prefix messages with length

public class BluetoothCommon {

    public static final String TAG = "BluetoothCommon";

    public static BluetoothSocket connect(BluetoothDevice device, Set<UUID> uuids, int attemptsPerUuid) throws IOException {
        Log.v(TAG, "connect()");

        BluetoothSocket socket = null;
        // Try one UUID at a time, a few times each until one connects
        for (UUID uuid : uuids) {
            for (int attempt = 0; attempt < attemptsPerUuid; attempt++) {
                try {
                    Log.i(TAG, BluetoothAdapter.getDefaultAdapter().getName() + " trying to connect to " + device.getName() + " using UUID " + uuid + " (attempt " + attempt + ")");
                    socket = device.createRfcommSocketToServiceRecord(uuid);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    socket.connect();
                } catch (IOException e) {
                    Log.w(TAG, BluetoothAdapter.getDefaultAdapter().getName() + " failed to connect to " + device.getName() + " using UUID " + uuid + " (attempt " + attempt + ")");
                    continue;
                }
                break;
            }
            if (socket.isConnected()) {
                break;
            }
        }
        if (!socket.isConnected()) {
            throw new IOException(BluetoothAdapter.getDefaultAdapter().getName() + " failed to connect to " + device.getName());
        }
        return socket;
    }

    public static String messageToString(Message message) {

        String result = "<Failed to convert message to string>";
        try {
        File file = new File(Environment.getExternalStorageDirectory() + "/message.txt");
        FileOutputStream fis = new FileOutputStream(file);
        MessageWriter writer = new DefaultMessageWriter();
        writer.writeMessage(message, fis);
        return FileUtils.readFileToString(file);
        } catch (IOException e) {
            Log.wtf(TAG, "Failed to convert message to string", e);
        }
        return result;

    }

    public static void write(JSONObject jo, DataOutputStream bluetoothOut) throws IOException {
        Log.v(TAG, "write()");
        byte[] buffer = jo.toString().getBytes("UTF-8");
        bluetoothOut.writeInt(buffer.length);
        bluetoothOut.write(buffer);
    }

    public static void write(File file, DataOutputStream bluetoothOut) throws IOException {
        Log.v(TAG, "write()");
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Failed to write file. File too long.");
        }
        bluetoothOut.writeInt((int) length);
        IOUtils.copy(new FileInputStream(file), bluetoothOut);
    }

    private static byte[] read(DataInputStream bluetoothIn) throws IOException {
        Log.v(TAG, "read()");

        // Read appropriate part from the Bluetooth stream
        int length = bluetoothIn.readInt();
        byte[] buffer = new byte[length];
        bluetoothIn.read(buffer);
        return buffer;
    }

    public static JSONObject readJson(DataInputStream bluetoothIn) throws IOException, JSONException {
        byte[] buffer = read(bluetoothIn);
        String json = new String(buffer, "UTF-8");
        return new JSONObject(json);
    }

    public static byte[] readFile(Ndo ndo, DataInputStream bluetoothIn) throws IOException {
        // TODO Don't read entire file into memory
        return read(bluetoothIn);
    }

}
