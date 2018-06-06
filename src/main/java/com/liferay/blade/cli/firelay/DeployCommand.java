package com.liferay.blade.cli.firelay;
import com.liferay.blade.cli.*;
import com.liferay.blade.cli.gradle.GradleExec;
import com.liferay.blade.cli.gradle.GradleTooling;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DeployCommand extends BaseCommand<DeployOcCommandArgs>{

    public DeployCommand(BladeCLI blade, DeployOcCommandArgs args){
        super(blade, args);

    }

    public DeployCommand(){}

    @Override
    public Class<DeployOcCommandArgs> getArgsClass() {
        return DeployOcCommandArgs.class;
    }

    @Override
    public void execute() throws Exception {
        if(_verifyPodStatus()){
            System.out.println("Building deployment files...");
            GradleExec gradleExec = new GradleExec(_blade);
            Set<File> outputFiles = GradleTooling.getOutputFiles(_blade.getCacheDir(), _blade.getBase());
            if (_args.isWatch()) {
                _deployWatch(gradleExec, outputFiles);
            }
            else {
                _deploy(gradleExec, outputFiles);
            }
        }else{
            System.out.println("Liferay container is not ready to receive deployments");
        }
    }

    private Boolean _verifyPodStatus(){
        Boolean isReady = false;
        String line = "";
        try{
            String[] arguments = new String[] {"oc", "get", "pods", "-o", "jsonpath='{.items[0].status.containerStatuses[0].ready}'", "-l", "tier=portal"};
            ProcessBuilder pb = new ProcessBuilder(arguments);
            Process p = pb.start();
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine())!= null) {
                if(line.equals("true") || line.equals("'true'")){
                    isReady = true;
                }
            }
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return isReady;
    }

    private void _addError(String msg) {
        _blade.addErrors("deploy", Collections.singleton(msg));
    }

    private void _deploy(GradleExec gradle, Set<File> outputFiles) throws Exception {
        int retcode = gradle.executeGradleCommand("assemble -x check");
        if (retcode > 0) {
            _addError("Gradle assemble task failed.");
            return;
        }
        Stream<File> stream = outputFiles.stream();

        stream.filter(
                File::exists
        ).forEach(
                outputFile -> {
                    try {
                        _installOrUpdate(outputFile);
                    }
                    catch (Exception e) {
                        PrintStream err = _blade.err();
                        err.println(e.getMessage());
                        e.printStackTrace(err);
                    }
                }
        );
    }

    private void _deployWatch(final GradleExec gradleExec, final Set<File> outputFiles) throws Exception {
        _deploy(gradleExec, outputFiles);
        Stream<File> stream = outputFiles.stream();
        Collection<Path> outputPaths = stream.map(
                File::toPath
        ).collect(
                Collectors.toSet()
        );

        new Thread() {

            @Override
            public void run() {
                try {
                    gradleExec.executeGradleCommand("assemble -x check -t");
                }
                catch (Exception e) {
                }
            }

        }.start();

        FileWatcher.Consumer<Path> consumer = new FileWatcher.Consumer<Path>() {

            @Override
            public void consume(Path modified) {
                try {
                    File file = modified.toFile();
                    File modifiedFile = file.getAbsoluteFile();
                    if (outputPaths.contains(modifiedFile.toPath())) {
                        _installOrUpdate(modifiedFile);
                    }
                }
                catch (Exception e) {
                }
            }

        };

        File base = _blade.getBase();
        new FileWatcher(base.toPath(), true, consumer);
    }

    private void _installOrUpdate(File file) throws Exception {
        file = file.getAbsoluteFile();
        System.out.println("Deploying... ");
        uploadToPod(file);
    }

    private void uploadToPod(File bundle){
        String podName = _args.getPodName();
        String parent = bundle.getParent();
        if(podName == null || podName.isEmpty()){
            podName = getPodName();
        }
        System.out.println("Deploying to POD -> " + podName);
        if(!podName.isEmpty() && podName != null){
            String cmdString = "rsync " + parent + "/ " + podName + ":/opt/liferay/deploy";
            try{
                Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
                process.waitFor();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }else{
            System.out.println("INVALID POD NAME");
        }
    }

    private String getPodName(){
        String name = "";
        String line = "";
        try{
            String[] arguments = new String[] {"oc", "get", "pods", "-o", "name", "-l", "tier=portal"};
            ProcessBuilder pb = new ProcessBuilder(arguments);
            Process p = pb.start();
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine())!= null) {
                name = line;
            }
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        if(name.length() > 0 && name.contains("/")){
            int index = name.indexOf("/");
            name = name.substring(index + 1, name.length());
        }
        return name;
    }

    private void checkForRemote(){
        File propertiesFile = new File(_blade.getBase().getAbsolutePath() + "/configs/fireBlade/env.properties");
        try{
            Properties prop = new Properties();
            InputStream input;
            if(propertiesFile.exists()){
                input = new FileInputStream(new File(propertiesFile.getAbsolutePath()));
                prop.load(input);
                String remote = prop.getProperty("openshift.remote.url");
                String user = prop.getProperty("openshift.user");
                String pass = prop.getProperty("openshift.pass");
                System.out.println("Getting remote OC");
                if(remote != null){
                    checkAuthentication(remote, user, pass);
                }
            }else{
                System.out.println("env.properties file does not exists");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void checkAuthentication(String host, String user, String pass){
        Boolean isValid = false;
        String line = "";
        try{
            String[] arguments = new String[] {"oc", "whoami"};
            ProcessBuilder pb = new ProcessBuilder(arguments);
            Process p = pb.start();
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine())!= null) {
                if(line.equals(user)){
                    isValid = true;
                }
            }
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        if(!isValid){
            remoteLogin(host, user, pass);
        }
    }

    private void remoteLogin(String host, String user, String pass){
        String name = "";
        String line = "";
        try{
            String[] arguments = new String[] {"oc", "login", host, "-u", user, "-p", pass, "--insecure-skip-tls-verify"};
            ProcessBuilder pb = new ProcessBuilder(arguments);
            Process p = pb.start();
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine())!= null) {
                System.out.println(line);
            }
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }



}


