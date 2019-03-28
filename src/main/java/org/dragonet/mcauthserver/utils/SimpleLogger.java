package org.dragonet.mcauthserver.utils;


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on 2017/9/25.
 */
public class SimpleLogger {

    private List<OutputStream> outs = new ArrayList<OutputStream>();

    public void addOutput(OutputStream out) {
        synchronized (this) {
            outs.add(out);
        }
    }

    public void info(String str) {
        log(LogLevel.INFO, str);
    }

    public void warning(String str) {
        log(LogLevel.WARNING, str);
    }

    public void severe(String str) {
        log(LogLevel.SEVERE, str);
    }

    public void log(LogLevel level, String str) {
        println(prefix() + " " + level.name() + " " + str);
    }

    public String prefix() {
        return DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
    }

    public void println(String str) {
        for(OutputStream o : outs) {
            synchronized (o) {
                try {
                    o.write(str.getBytes(StandardCharsets.UTF_8));
                    o.write("\n".getBytes());
                    o.flush();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
