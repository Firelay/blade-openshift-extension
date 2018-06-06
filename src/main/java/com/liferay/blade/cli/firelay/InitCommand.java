package com.liferay.blade.cli.firelay;

import com.liferay.blade.cli.BaseCommand;
import com.liferay.blade.cli.BladeCLI;
import com.liferay.blade.cli.Util;
import com.liferay.blade.cli.firelay.util.FireBladeConfUtil;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.net.URL;
import java.util.Properties;


public class InitCommand extends BaseCommand<InitOcCommandArgs>{

    public InitCommand(BladeCLI blade, InitOcCommandArgs args){
        super(blade, args);
    }

    public InitCommand(){}

    @Override
    public Class<InitOcCommandArgs> getArgsClass() {
        return InitOcCommandArgs.class;
    }

    @Override
    public void execute() throws Exception {
        String name = _args.getName();
        if(name != null && !name.isEmpty()){
            if(!name.matches("^[a-z0-9]+")){
                System.out.println("Error: Name must be lowercase and only alphanumeric");
                return;
            }
        }
        try{
            System.out.println("Creating workspace");
            //BLADE DEFAULT INIT
            new com.liferay.blade.cli.InitCommand(_blade, _args).execute();
        }catch (Exception bladeEx){
            bladeEx.printStackTrace();
        }
        //Fire Blade impl
        if(FireBladeConfUtil.isValidWorkspace(_blade.getBase().getAbsolutePath(), name)){
            createOcConfPath();
        }
    }

    private void createOcConfPath(){
        System.out.println("Creating configuration files");
        File confDir;
        if(_args.getName() == null){
            confDir = new File(_blade.getBase().getAbsolutePath() + "/configs");
        }else{
            confDir = new File(_blade.getBase().getAbsolutePath() + SystemUtils.FILE_SEPARATOR + _args.getName() +"/configs");
        }
        if(confDir.exists() && confDir.isDirectory()){
            File ocConfDir = new File(confDir, "oc");
            if(!ocConfDir.exists()){
                ocConfDir.mkdir();
                _trace("Creating oc configuration file");
                podFiles(confDir.getAbsolutePath());
            }
        }else{
            _trace("Creating Conf Directory");
            confDir.mkdir();
            createOcConfPath();
        }
        FireBladeConfUtil.ignoreEnvProperties(confDir.getAbsolutePath());
    }

    private void podFiles(String baseDir){
        URL envFile = this.getClass().getClassLoader().getResource("com/liferay/blade/cli/firelay/env.properties");
        FireBladeConfUtil.createPodConfFiles(baseDir, envFile, "env.properties");
        URL mysql = this.getClass().getClassLoader().getResource("com/liferay/blade/cli/firelay/mysql.yaml");
        URL liferay = this.getClass().getClassLoader().getResource("com/liferay/blade/cli/firelay/liferay7.yaml");
        URL jenkins = this.getClass().getClassLoader().getResource("com/liferay/blade/cli/firelay/jenkins.yaml");
        URL es = this.getClass().getClassLoader().getResource("com/liferay/blade/cli/firelay/elasticsearch.yaml");
        FireBladeConfUtil.createPodConfFiles(baseDir, mysql, "mysql.yaml");
        FireBladeConfUtil.createPodConfFiles(baseDir, liferay, "liferay7.yaml");
        FireBladeConfUtil.createPodConfFiles(baseDir, jenkins, "jenkins.yaml");
        FireBladeConfUtil.createPodConfFiles(baseDir, es, "elasticsearch.yaml");
        System.out.println("Completed... you can now configure your desired environment on /configs/oc/env.properties");
    }

    private void _trace(String msg) {
        _blade.trace("%s: %s", "init", msg);
    }


}
