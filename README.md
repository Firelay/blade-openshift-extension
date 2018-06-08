# blade-openshift-extension

The blade-openshift-extension project is a Blade CLI extension that helps you implement the required commands for you to connect, create and deploy to your openshift environments.

## Prerequisites

You will need to have installed on your computer the following items:

- [Blade CLI](https://dev.liferay.com/en/develop/tutorials/-/knowledge_base/7-0/installing-blade-cli)
- [Minishift](https://github.com/minishift/minishift)

## Installing

- Clone the repository 
- Open your terminal and run the following command by replacing the path with your downloaded project path:

```
blade extension install ${path}
```

This will install and enable the blade-openshift-extension on the Blade CLI and let us work with our new commands.   


## blade-openshift-extension commands:

When the extension is installed we are going to have the following commands available:

- init-oc
- start-oc
- deploy-oc 

You should be able to verify that the extension is correctly installed by listing the available commands on Blade, you can do this by running on your terminal:

```
blade -help
```

### init-oc 

The command ```init-oc``` is going to create your workspace along with the oc configuration file required to start or connect to your openshift environments. 
You can also specify the name of your project appending to the command

```
blade init-oc myProject
```

This will create a folder named myProject with your workspace insde, this will also be used as the name of your project on openshift. 

After running this command you will need to go to `/configs/oc/` and edit your `env.properties` to define your openshift variables and also you can specify a git configuration that will help you create a [GitHub trigger](#github-triggers).

If no properties are set a minishift will be started. 

### start-oc

The command ```start-oc``` will create your openshift environment depending on the established properties on the env.properties file.
 
If a remote host URL is defined it will connect to your openshift and create your workspace otherwise it will start a minishift and install your environment locally.

### deploy-oc

The command ```deploy-oc``` deploys your modules/wars to your current active openshift project.
 
If you need to deploy only one and not the whole bundle, go to the root folder of your module/war and run the command, if you need to deploy all modules/wars run the command on the root of your project.
 
If you have access to your cluster and can identify the name of the pod where your Liferay is running you can also deploy directly to this pod by running the command with the pod name:

```
blade deploy-oc -p podName
```

### Deploy watch

If you don't want to be running the command every time you change something in your code, you can run the command with a watcher, this will start monitoring your workspace directory listening for changes.

If a change is detected a build will be trigger followed by a deploy to your Liferay pod without the need for you to constantly be running the command.

To deploy and start watching your workspace directory run the following command:

```
blade deploy-oc -w
``` 
 

## Templates

Along with your env.properties file on the configuration folder, you will find the templates needed to create your workspace cluster (Liferay, MySql, Elastic Search and Jenkins instances). 

You can modify these templates and then apply them to your openshift project.


##Jenkins
Openshift allows us to create a build configuration with a pipeline strategy, this means that we are able to instruct jenkins with a pipeline file and indicate the required stages or steps needed that  our builds needs to run.


##GitHub triggers

Jenkins allows you to trigger a build when defined actions occur, in this case we are going to trigger a build by pushing a commit to our repository with GitHub. 

First we need to create a Webhook to our GitHub and for that we need a secret. 
Secrets for Webhook URLs can be added on our Build Configuration template, in this case we have an example on the Liferay.yaml template, and it looks like this: 

```
triggers:
    - type: "GitHub"
      github:
        secret: myUrlSecret
      type: GitHub
```

We are defining a secret `myUrlSecret` for our GitHub trigger, you can specify a different secret for your GitHub trigger.

When you have already define you secret and started your Openshift project, head to your project on Openshift, go to `Builds > Pipelines > Configuration tab`, here you will have a Triggers section where your GitHub Webhook URL will be displayed. 

Copy that URL and head to your GitHub project, create a Webhook, add the URL that you just copy and enter the defined secret for your webhook. Save it and you have configured a trigger for your project. 

Now you can start your Jenkins builds from Openshift or you can also push code to your repository and a build will be triggered.   
 


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

