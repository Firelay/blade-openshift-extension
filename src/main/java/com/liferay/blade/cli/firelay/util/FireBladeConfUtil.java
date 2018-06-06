package com.liferay.blade.cli.firelay.util;

import com.liferay.blade.cli.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;


import java.io.*;
import java.net.URL;


public class FireBladeConfUtil {

    static public void createPodConfFiles(String baseDir, URL filePath, String name){
        File destDir = new File(baseDir + "/oc");
        if(!destDir.exists()){
            destDir.mkdir();
        }
        File destFile = new File(baseDir + "/oc/" + name);
        if(!destFile.exists() && filePath != null){
            try{
                FileUtils.copyURLToFile(filePath, destFile);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    static public Boolean isValidWorkspace(String baseDir, String name){
        File workspaceDir;
        if(name != null && !name.isEmpty()){
            workspaceDir = new File(baseDir + SystemUtils.FILE_SEPARATOR + name + SystemUtils.FILE_SEPARATOR  + "build.gradle");
        }else{
            workspaceDir = new File(baseDir + SystemUtils.FILE_SEPARATOR + "build.gradle");
        }
        if(workspaceDir.exists()){
            return true;
        }else{
            return false;
        }
    }

    static public Boolean checkMinishift(){
        Boolean exists = false;
        String line = "";
        System.out.println("Checking for MiniShift...");
        if(Util.isWindows()){
            System.out.println("windows");
        }else{
            try{
                String[] arguments = new String[] {"minishift", "version"};
                ProcessBuilder pb = new ProcessBuilder(arguments);
                Process p = pb.start();
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = reader.readLine())!= null) {
                    System.out.println(line);
                    if(line.contains("minishift v")){
                        exists = true;
                    }
                }
                while ((line = stdError.readLine()) != null) {
                    System.out.println(line);
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        return exists;
    }

    static public Boolean checkOc(){
        Boolean exists = false;
        String line = "";
        System.out.println("Checking for OC...");
        if(Util.isWindows()){
            System.out.println("windows");
        }else{
            try{
                String[] arguments = new String[] {"oc", "version"};
                ProcessBuilder pb = new ProcessBuilder(arguments);
                Process p = pb.start();
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = reader.readLine())!= null) {
                    System.out.println(line);
                    if(line.contains("oc v")){
                        exists = true;
                    }
                }
                while ((line = stdError.readLine()) != null) {
                    System.out.println(line);
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        return exists;
    }

    static public void ignoreEnvProperties(String basePath){
        String path = basePath.replace("/configs", "");
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            String data = "\n**/env.properties";
            File file = new File(path + SystemUtils.FILE_SEPARATOR + "/.gitignore");
            if(file.exists()){
                fw = new FileWriter(file.getAbsoluteFile(), true);
                bw = new BufferedWriter(fw);
                bw.write(data);
            }else{
                System.out.println(".gitignore not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


}
