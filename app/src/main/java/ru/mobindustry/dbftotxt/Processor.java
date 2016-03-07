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

    public static String[] ENCODINGS = new String[]{
            "IBM00858",
            "IBM437",
            "IBM775",
            "IBM850",
            "IBM852",
            "IBM855",
            "IBM857",
            "IBM862",
            "IBM866",
            "ISO-8859-1",
            "ISO-8859-2",
            "ISO-8859-4",
            "ISO-8859-5",
            "ISO-8859-7",
            "ISO-8859-9",
            "ISO-8859-13",
            "ISO-8859-15",
            "KOI8-R",
            "KOI8-U",
            "US-ASCII",
            "UTF-8",
            "UTF-16",
            "UTF-16BE",
            "UTF-16LE",
            "UTF-32",
            "UTF-32BE",
            "UTF-32LE",
            "x-UTF-32BE-BOM",
            "x-UTF-32LE-BOM",
            "windows-1250",
            "windows-1251",
            "windows-1252",
            "windows-1253",
            "windows-1254",
            "windows-1257",
            "x-IBM737",
            "x-IBM874",
            "x-UTF-16LE-BOM"
    };

    OutputStream outputStream;
    String inputEncoding;
    String outputEncoding;

    volatile int counter = 0;

    volatile boolean finish = false;

    public Processor(OutputStream output, String inputEncoding, String outputEncoding){
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

    public void close() {
        finish=true;
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCount(){
        return counter;
    }
}