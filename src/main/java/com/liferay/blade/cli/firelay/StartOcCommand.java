package com.liferay.blade.cli.firelay;

import com.liferay.blade.cli.BaseCommand;
import com.liferay.blade.cli.BladeCLI;
import com.liferay.blade.cli.Util;
import com.liferay.blade.cli.firelay.util.FireBladeConfUtil;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.util.Properties;

public class StartOcCommand extends BaseCommand<StartOcCommandArgs> {
    public StartOcCommand(BladeCLI blade, StartOcCommandArgs args){
        super(blade, args);

    }

    public StartOcCommand(){}

    @Override
    public Class<StartOcCommandArgs> getArgsClass() {
        return StartOcCommandArgs.class;
    }


    @Override
    public void execute() {
        checkForRemote();
    }

    private void verifyEnvs(){
        if(!FireBladeConfUtil.checkMinishift()){
            System.out.println("Minishift isn't installed, please download the latest version of Minishift if you want to run your project locally:");
            System.out.println("https://github.com/minishift/minishift");
            System.out.println("Or you can also configure your openshift properties on /configs/oc/env.properties to run it remotly");
            return;
        }
        if(!FireBladeConfUtil.checkOc()){
            System.out.println("The command oc was not found.  Make sure it is correctly configured.  You can run 'minishift oc-env' and follow the instructions to configure it.");
            return;
        }
        startMinishift();
    }

    private void startMinishift(){
        try {
            String minishiftCommand = "minishift";
            String cmdString= "start";
            _trace(cmdString);
            Process process = Util.startProcess(_blade, "\"" + minishiftCommand + "\" " + cmdString, null, false);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loginAsAdmin();
    }

    private void loginAsAdmin(){
        String user = "system:admin";
        String cmdString = "login -u " + user;
        _trace(cmdString);
        try{
            Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
            process.waitFor();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        checkProjects();
    }

    private void checkProjects(){
        System.out.println("Checking existing projects...");
        String line = "";
        Boolean exists = false;
        try{
            String[] arguments = new String[] {"oc", "projects"};
            ProcessBuilder pb = new ProcessBuilder(arguments);
            Process p = pb.start();
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine())!= null) {
                if(line.equals(projectName())){
                    exists = true;
                }
            }
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        if(!exists){
            addProject(false);
        }else{
            useProject();
        }
    }

    private void useProject(){
        String cmdString = "project " + projectName();
        try{
            Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
            process.waitFor();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    private void addProject(boolean isRemote){
        String cmdString = "new-project " + projectName();
        if(isRemote){
            cmdString = "new-project " + _blade.getBase().getName();
        }
        try{
            Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
            process.waitFor();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        if(!isRemote){
            loadPermissions();
            addUserToProject();

        }
        loadPods();
    }

    private void addUserToProject(){
        String cmdString = "policy add-role-to-user admin developer -n " + projectName();
        try{
            Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
            process.waitFor();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        loginAsDev();
    }

    private void loginAsDev(){
        String user = "developer";
        String pass = "developer";
        String cmdString = "login -u " + user + " -p " + pass;
        _trace(cmdString);
        try{
            Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
            process.waitFor();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void loadPods(){
        try{
            initJenkins();
            initElasticSearch();
            initMySqlPod();
            Thread.sleep(20000);
            initLiferayPod();
        }catch (Exception ex){
            ex.printStackTrace();
            Thread.currentThread().interrupt();
        }

    }

    private void loadPermissions(){
        String cmdString = "adm policy add-scc-to-user anyuid -z default";
        _trace(cmdString);
        try{
            Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
            process.waitFor();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private String projectName(){
        return _blade.getBase().getName();
    }


    private void initMySqlPod(){
        System.out.println("*** Deploying MySQL ***");
        File mysql = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + "configs" + SystemUtils.FILE_SEPARATOR + "oc" + SystemUtils.FILE_SEPARATOR + "mysql.yaml");
        if(mysql.exists()){
            String cmdString = "process -f " + mysql.getAbsolutePath() + " -p DEPLOYMENT_NAME=" + projectName() + " | oc apply -f -";
            try{
                Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
                process.waitFor();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }else{
            System.out.println("No template for MySQL was found on: " + mysql.getAbsolutePath());
        }

    }

    private void initLiferayPod(){
        System.out.println("*** Deploying Liferay ***");
        File propertiesFile = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + "configs" + SystemUtils.FILE_SEPARATOR + "oc"+ SystemUtils.FILE_SEPARATOR + "env.properties");
        try{
            Properties prop = new Properties();
            InputStream input;
            if(propertiesFile.exists()){
                input = new FileInputStream(new File(propertiesFile.getAbsolutePath()));
                prop.load(input);
                String gitHubUrl = prop.getProperty("github.url");
                String gitHubBranch = prop.getProperty("github.branch");
                File liferay = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + "configs" + SystemUtils.FILE_SEPARATOR + "oc" + SystemUtils.FILE_SEPARATOR + "liferay7.yaml");
                if(liferay.exists()){
                    String cmdString = "process -f " + liferay.getAbsolutePath() + " -p DEPLOYMENT_NAME=" + projectName() + " -p GITHUB_URL=" + gitHubUrl + " -p GITHUB_BRANCH=" + gitHubBranch + " | oc apply -f -";
                    try{
                        Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
                        process.waitFor();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }else{
                    System.out.println("No template for Liferay 7 was found on: " + liferay.getAbsolutePath());
                }
            }else{
                System.out.println("env.properties file does not exists");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    private void initJenkins(){
        System.out.println("*** Deploying Jenkins ***");
        File jenkins = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + "configs" + SystemUtils.FILE_SEPARATOR + "oc" + SystemUtils.FILE_SEPARATOR + "jenkins.yaml");
        if(jenkins.exists()){
            String cmdString = "process -f " + jenkins.getAbsolutePath() + " | oc apply -f -";
            try{
                Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
                process.waitFor();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }else{
            System.out.println("No template for Jenkins was found on: " + jenkins.getAbsolutePath());
        }
    }

    private void initElasticSearch(){
        System.out.println("*** Deploying Elastic Search ***");
        File es = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + "configs"+ SystemUtils.FILE_SEPARATOR + "oc"+ SystemUtils.FILE_SEPARATOR + "elasticsearch.yaml");
        if(es.exists()){
            String cmdString = "process -f " + es.getAbsolutePath() + " -p DEPLOYMENT_NAME=" + projectName() + " | oc apply -f -";
            try{
                Process process = Util.startProcess(_blade, "\"" + "oc" + "\" " + cmdString, null, false);
                process.waitFor();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }else{
            System.out.println("No template for Elastic Search was found on: " + es.getAbsolutePath());
        }
    }

    private void checkForRemote(){
        File propertiesFile = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + "configs" + SystemUtils.FILE_SEPARATOR + "oc"+ SystemUtils.FILE_SEPARATOR + "env.properties");
        try{
            Properties prop = new Properties();
            InputStream input;
            if(propertiesFile.exists()){
                input = new FileInputStream(new File(propertiesFile.getAbsolutePath()));
                prop.load(input);
                String remote = prop.getProperty("openshift.remote.url");
                String user = prop.getProperty("openshift.user");
                String pass = prop.getProperty("openshift.pass");
                String token = prop.getProperty("openshift.api.token");
                if(remote != null && !remote.isEmpty()){
                    checkAuthentication(remote, user, pass, token);
                }else{
                    System.out.println("No host found for Cloud Openshift");
                    verifyEnvs();
                }
            }else{
                System.out.println("env.properties file does not exists");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void checkAuthentication(String host, String user, String pass, String token){
        if(!token.isEmpty()){
            remoteLogin(host, user, pass, token);
        }else{
            if(user.isEmpty()){
                System.out.println("No user was specified on env.properties to connect to remote host");
                return;
            }
            if(pass.isEmpty()){
                System.out.println("No password was specified on env.properties to connect to remote host");
                return;
            }
            remoteLogin(host, user, pass, token);
        }
    }

    private void remoteLogin(String host, String user, String pass, String token){
        System.out.println("Connecting to cloud Openshift... ");
        String line = "";
        Boolean isError = false;
        try{
            String[] arguments = new String[]{};
            if(token.isEmpty()){
                arguments = new String[] {"oc", "login", host, "-u", user, "-p", pass, "--insecure-skip-tls-verify"};
            }else{
                arguments = new String[] {"oc", "login", "--token", token, "--insecure-skip-tls-verify"};
            }
            ProcessBuilder pb = new ProcessBuilder(arguments);
            Process p = pb.start();
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = reader.readLine())!= null) {
                System.out.println(line);
                if(line.contains("error")){
                    isError = true;
                }
            }
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
                isError = true;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        if(!isError){
            addProject(true);
        }

    }


    private void _trace(String msg) {
        _blade.trace("%s: %s", "init", msg);
    }

}
