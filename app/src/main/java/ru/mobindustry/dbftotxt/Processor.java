package ru.mobindustry.dbftotxt;

import org.jamel.dbf.processor.DbfRowProcessor;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Григорий on 09.02.2016.
 */
public class Processor implements DbfRowProcessor {

    private OutputStream outputStream;
    private String inputEncoding;
    private String outputEncoding;

    private volatile int counter = 0;

    Processor(OutputStream output, String inputEncoding, String outputEncoding){
        outputStream=output;
        this.inputEncoding=inputEncoding;
        this.outputEncoding=outputEncoding;
    }

    @Override
    public void processRow(Object[] row) {
        byte[] write = new byte[0];
        int i;
        double val;
        Date date;
        Object o;

        for(i=0; i<row.length; i++){
            o=row[i];
            if(o instanceof byte[]) {
                try {
                    String str = new String((byte[]) o, inputEncoding);
                    str = str.replaceAll("  +", "");
                    if(str.startsWith(" ")) str=str.substring(1);
                    if(str.endsWith(" ")) str=str.substring(0, str.length()-1);
                    write = str.getBytes(outputEncoding);
                } catch (Exception e) {
                    write = (byte[]) o;
                }
            } else if(o instanceof Float){

                write = ( o.toString() + "").getBytes();

            } else if(o instanceof Number) {

                val = (double) o;
                if(((double)((int)val))==val) write = (((int)val)+"").getBytes();
                else write = ( o.toString() + "").getBytes();

            } else if(o instanceof Date){

                date = (Date) o;
                String d = new SimpleDateFormat("dd.MM.yyyy").format(date);
                if(!d.equals("06.10.17793")) write = d.getBytes();
                else write=new byte[0];
            } else if(o instanceof Boolean){
                write=(((boolean)o)+"").getBytes();
            }
            try {
                outputStream.write(write);
                if(i!=row.length-1)
                    outputStream.write("\t".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            outputStream.write("\r\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        counter++;
    }

    void close() {
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int getCount(){
        return counter;
    }
}