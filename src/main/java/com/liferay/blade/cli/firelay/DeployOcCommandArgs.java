package com.liferay.blade.cli.firelay;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.liferay.blade.cli.BaseArgs;



@Parameters(commandDescription = "Builds and deploys bundles to an Openshift cluster.", commandNames = {"deploy-oc"})
public class DeployOcCommandArgs extends BaseArgs {

    public String getPodName() {
        return _podname;
    }

    @Parameter(
            description = "Pod name destination for deploy",
            names = {"-p", "--pod"}
    )
    private String _podname;

    public boolean isRemote() {
        return _remote;
    }

    @Parameter(
            description = "Deploys to a remote host", names = {"-r", "--remote"}
    )
    private boolean _remote;

    public boolean isWatch() {
        return _watch;
    }

    @Parameter(
            description = "Watches the deployed files for changes and will automatically redeploy", names = {"-w", "--watch"}
    )
    private boolean _watch;

}
