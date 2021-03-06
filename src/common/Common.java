/*
 * CAL.
 *  A P2P chat program that lets you communicate without any infrastructure.
 *
 *   Copyright (C) 2015  Foo-Manroot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package common;

import commands.Parser;
import control.ConnectionObserver;
import files.FileObserver;
import gui.utils.LangChangeObserver;
import gui.main.PeerGUI;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import networking.NetUtils;
import packets.ControlMessage;
import peer.Host;

/**
 * Default parameters for the message interchange and other common static
 * methods.
 */
public class Common {

    /**
     * Size of the buffer (in Bytes).
     */
    public static final int BUFF_SIZE = 2048;

    /**
     * Route to the default file where the hosts information will be stored.
     */
    public static final String FILE_PATH = "./known_hosts";

    /**
     * Reserved data flow to indicate, for example, that a peer is indifferent
     * about the chosen data flow ID on a {@code CHNG_DF_REQ}
     * {@link ControlMessage}.
     */
    public static final byte RESERVED_DATA_FLOW = 127;

    /**
     * Object used to log events.
     */
    public static final Logger logger = Logger.newLogger();
    
    /**
     * Observer for the file transfers.
     */
    public static final FileObserver fileObserver = FileObserver.newObserver();
    
    /**
     * Current room of
     */
    public static byte currentRoom = RESERVED_DATA_FLOW;

/* GUI-RELATED ATTRIBUTES */
    /**
     * Name of the packet with the strings on different languages.
     */
    public static String resourceBundle = "resources.Language";

    /**
     * Name of the FXML document for the GUI application.
     */
    public static String fxmlDocument = "FXMLPeer.fxml";

    /**
     * Observer for the GUI application to know when a new connection is
     * accepted.
     */
    public static ConnectionObserver connectionObserver = new ConnectionObserver();

    /**
     * Parser to translate and execute the supported commands.
     */
    public static Parser parser = new Parser ();

    /**
     * Escape character for the commands. If a string begins with this
     * character, it will be parsed as a command.
     */
    public static char escapeChar = '/';

    /**
     * Observer to change the language on the option buttons of the chat panel.
     */
    public static LangChangeObserver langObserver = new LangChangeObserver();

/* ----------------- */
/* ---- METHODS ---- */
/* ----------------- */

    /**
     * Logs the given message.
     *
     * @param message
     *              Message to be logged.
     *
     * @deprecated
     *              This functionality is now provided by {@link Logger}
     * @see Logger
     */
    public static synchronized void log(String message) {

        System.out.print(message);
    }

    /**
     * Opens a data file an returns its content.
     *
     * @param path
     *              Path to the file.
     *
     * @return
     *              The content of the file on a new object. If the file didn't
     *          exists or no objects are stored there, an empty {@link ArrayList} is
     *          returned.
     */
    public synchronized static ArrayList<Object> openDatFile(String path) {

        ArrayList<Object> content = new ArrayList<>();
        Object obj;

        boolean cont = true;
        try {
            try (FileInputStream fis = new FileInputStream(FILE_PATH)) {
                ObjectInputStream ois = new ObjectInputStream(fis);
                while (cont) {

                    obj = ois.readObject();

                    if (obj != null) {

                        content.add(obj);
                    } else {

                        cont = false;
                    }
                }
            }

        } catch (EOFException ex) {
            /* Exception thrown while reading at the end of the file. This is
             done to check wether all objects where read or not. */
        } catch (IOException | ClassNotFoundException ex) {

            logger.logError("Exception at Common.openDatFile(): "
                    + ex.getMessage() + "\n");
            return null;
        }

        return content;

    }

    /**
     * Stores the given object on the specified file.
     *
     * @param path
     *              Path to the file.
     *
     * @param content
     *              Object to be stored
     *
     * @param append
     *              If this parameter is <i>true</i>, the given content will be
     *          appended to the previous one of the file (if any). If it's
     *          <i>false</i>, the content will be overridden.
     *
     * @return
     *              <i>true</i> if the content was stored successfully,
     *          <i>false</i> if any error occurred.
     */
    public synchronized static boolean storeDatFile(String path,
                                                    Object content,
                                                    boolean append) {

        File f = new File(path);
        /* Different ObjectOutputStreams have different headers, so an error
         occurs when trying to read a file appended with two different
         ObjectOutputStreams. That's why the previous content of the file
         is retrieved to write it again with the same header */
        ArrayList<Object> previousContent = null;

        if (append &&
            f.exists() &&
            f.isFile()) {

            previousContent = openDatFile(path);
        }

        try {
            try (
                    FileOutputStream fos = new FileOutputStream(path);
                    ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                /* Discards the previous state of the ObjectOutputStream */
                oos.reset();

                if (previousContent != null) {

                    for (Object o : previousContent) {

                        oos.writeObject(o);
                    }
                }

                oos.writeObject(content);
            }

        } catch (IOException ex) {

            logger.logError("Exception at Common.storeDatFile(): "
                    + ex.getMessage() + "\n");
            return false;
        }

        return true;
    }

    /**
     * Converts the given port number (an <i>integer</i> of 4 bytes) into a byte
     * array with 4 positions. On the first one ({@code array[0]}) will be the
     * highest byte of the integer.
     *
     * @param port
     *              Port number to be converted.
     *
     *
     * @return
     *              A byte array with the highest part of the number on
     *          {@code array[0]}.
     */
    public static byte[] intToArray(int port) {

        byte[] portArray = new byte[4];

        ByteBuffer aux = ByteBuffer.allocate(portArray.length);
        aux.putInt(port);
        portArray = aux.array();

        return portArray;
    }

    /**
     * Converts the given number (stored in the array of 4 bytes) into an
     * integer. On {@code array[0]} will be the highest byte of the integer.
     *
     * @param array
     *              A byte array with the highest part of the number on
     *          {@code array[0]}.
     *
     *
     * @return
     *              An integer whose value equals the one stored on the array.
     */
    public static int arrayToInt(byte[] array) {
        int port;
        int[] intArray = new int[4];

        if (array.length != intArray.length) {

            return -1;
        }

        /* Stores the bytes into an array of integers */
        for (int i = 0; i < intArray.length; i++) {

            intArray[i] = Byte.toUnsignedInt(array[i]);
        }

        /* To convert the two bytes into one int, the bytes must be
         displaced to their correct position */
        port = (intArray[0] << 24);
        port += (intArray[1] << 16);
        port += (intArray[2] << 8);
        port += intArray[3];

        return port;
    }

    /**
     * Checks whether the given host is the same as the local peer (the static
     * attribute {@code peer} on {@link PeerGUI}).
     *
     *
     * @param host
     *              The host to be checked.
     *
     *
     * @return
     *              <i>true</i> if the IP address and the port matches;
     *          <i>false</i> otherwise.
     */
    public static boolean isLocalPeer (Host host) {

        return (
                NetUtils.getInterfaces().contains(host.getIPaddress()) &&
                host.getPort() == PeerGUI.peer.getServer().getSocket().getLocalPort()
                );
    }

}
