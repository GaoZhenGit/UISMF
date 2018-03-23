package com.gz;

import java.io.*;

/**
 * Created by host on 2018/2/1.
 */
public class CommandUtil {

    public static final String COMMAND_EXIT     = "exit\n";
    public static final String COMMAND_LINE_END = "\n";

    private static final String WINDOWS_CMD = "cmd";
    private static final String LINUX_CMD   = "sh";

    public static String lastError;

    public static boolean exec(String... commands) {
        lastError = "";
        int result = -1;

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;

        DataOutputStream os = null;
        try {

            String commandLine = generateCommandLine(commands);

            System.out.println("CommandUtil:" + commandLine);

            if (isWindows()) {
                process = Runtime.getRuntime().exec(WINDOWS_CMD);
            } else {
                process = Runtime.getRuntime().exec(LINUX_CMD);
            }
            os = new DataOutputStream(process.getOutputStream());

            if (commands == null) {
                return false;
            }

            os.write(commandLine.getBytes());
            os.writeBytes(COMMAND_LINE_END);
            os.flush();

            os.writeBytes(COMMAND_EXIT);
            os.flush();

            // get command result
            if (true) {
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                new ReaderThread(successResult).start();
                new ReaderThread(errorResult, true).start();
            }

            result = process.waitFor();

            if (result != 0) {
                if (process.exitValue() == 1) {
                    System.err.println("命令执行失败!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            close(os);
            close(successResult);
            close(errorResult);

            if (process != null) {
                process.destroy();
            }
        }
        return result == 0;
    }

    private static class ReaderThread extends Thread {
        BufferedReader reader;
        boolean isError;

        ReaderThread(BufferedReader bufferedReader, boolean isError) {
            reader = bufferedReader;
            this.isError = isError;
        }

        ReaderThread(BufferedReader bufferedReader) {
            this(bufferedReader, false);
        }

        @Override
        public void run() {
            String s;
            StringBuilder stringBuilder = new StringBuilder();
            try {
                while ((s = reader.readLine()) != null) {
                    if (isError) {
                        System.err.println(s);
                        stringBuilder.append(s);
                    } else {
                        System.out.println(s);
                    }
                }
                lastError = stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String generateCommandLine(String[] commands) {
        StringBuilder sb = new StringBuilder();
        for(String cmd: commands){
            sb.append(cmd).append(" ");
        }
        return sb.toString();
    }

    public static boolean isWindows(){
        String osName = System.getProperty("os.name");
        if(osName != null && osName.toLowerCase().contains("windows")){
            return true;
        }
        return false;
    }

    public static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
