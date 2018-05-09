package io;

import com.google.common.base.Joiner;
import reconstruction.containers.ReconstrucedEvent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;


/**
 * Writes results to a csv file.
 */
public class CSVWriter implements Serializable{
    private PrintWriter writer;
    private String seperator = ",";

    private boolean headerWritten = false;

    /**
     * Create a new CSVWriter for the given File.
     * @param file the file to create.
     * @throws IOException in case the file can not be written to.
     */
    public CSVWriter(File file) throws IOException {
        writer = new PrintWriter(file);
    }

    /**
     * Appends a row to the CSV file. Containing the values for
     *
     *    e.eventID,
     *    e.direction.getX(),
     *    e.direction.getY(),
     *    e.direction.getZ(),
     *    e.impactPosition.getX(),
     *    e.impactPosition.getY(),
     *    classPrediction
     *
     *
     * @param e the reconstructed event object
     * @param classPrediction the prediction (aka. gammaness)
     * @throws IOException in case the file cannot not be written to.
     */
    public void append(ReconstrucedEvent e, double classPrediction) throws IOException {
        if(!headerWritten){
            writeHeader("id", "direction_x", "direction_y", "direction_z", "position_x", "position_z", "prediction");
            headerWritten = true;
        }
        String s = Joiner.on(seperator).join(
                    e.eventID,
                    e.direction.getX(),
                    e.direction.getY(),
                    e.direction.getZ(),
                    e.impactPosition.getX(),
                    e.impactPosition.getY(),
                    classPrediction
        );
        writer.println(s);
        writer.flush();
    }



    /**
     * Appends a row to the CSV file. Containing the values for
     *
     *    e.eventID,
     *    mc.energy
     *    mc.impact
     *    e.direction.getX(),
     *    e.direction.getY(),
     *    e.direction.getZ(),
     *    e.impactPosition.getX(),
     *    e.impactPosition.getY(),
     *    classPrediction
     *
     * @param event the event object returned from the ImageReader
     * @param reconstrucedEvent the reconstructed event object
     * @param classPrediction the prediction (aka. gammaness)
     * @throws IOException in case the file cannot not be written to.
     */
    public void append(ImageReader.Event event, ReconstrucedEvent reconstrucedEvent, double classPrediction) throws IOException {
        if(!headerWritten){
            writeHeader("id","mc_alt", "mc_az", "mc_energy","mc_core_x", "mc_core_y",  "alt", "az", "core_x", "core_y", "prediction");
            headerWritten = true;
        }
        String s = Joiner.on(seperator).join(
                event.eventId,
                event.mc.alt,
                event.mc.az,
                event.mc.energy,
                event.mc.coreX,
                event.mc.coreY,
                reconstrucedEvent.alt,
                reconstrucedEvent.az,
                reconstrucedEvent.impactPosition.getX(),
                reconstrucedEvent.impactPosition.getY(),
                classPrediction
        );
        writer.println(s);
        writer.flush();
    }


    /**
     * Same as {@link CSVWriter#append }just without throwing a checked exception
     *
     * @param e same as {@link CSVWriter#append}
     * @param classPrediction same as {@link CSVWriter#append}
     */
    public void appendUnchecked(ReconstrucedEvent e, double classPrediction){
        try {
            append(e, classPrediction);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }


    /**
     * Write the given strings as a row to the CSV file. useful for writing the header.
     * @param headerKeywords the strings to write into the row.
     */
    public void writeHeader(String... headerKeywords) {
        String h = Joiner.on(seperator).join(headerKeywords);
        writer.println(h);
        writer.flush();
    }
}
