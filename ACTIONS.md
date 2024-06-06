# GitHub Actions setup

Quarkus is built using GitHub Actions, with a mix of hosted and self-hosted runners.

## Setting up a new self-hosted runner on Mac M1

### Non-root user

GitHub Actions should not run with administrator privileges, so will need to be run in a dedicated account.
Make an account for the actions runner. The steps below assume the account has the name `githubactions`.
It's usually easiest to do account creation in the GUI, via a VNC connection. Users are managed in
the **Users and Groups** section of the Settings app.

*Grant administrator privileges to the account* (we will remove them later).

### System utilities

As administrator, install homebrew.

```shell
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Install build and container utilities:

```bash
brew install mvn
brew install gradle
brew install podman
```

### Podman setup

Podman needs some [extra configuration](https://quarkus.io/guides/podman) to work with test containers and dev services.

Now log in to the account you created to run the actions.
As the `githubactions` user (but with administrator privileges)

```bash
PODMAN_VERSION=`podman -v | sed 's/[a-zA-Z ]*//'`
sudo /opt/homebrew/Cellar/podman/$PODMAN_VERSION/bin/podman-mac-helper install
```

The podman helper install seems to be per-user, but it needs to be done with administrator privileges.

Again as the `githubactions` user, edit ~/.testcontainers.properties and add the following line: `ryuk.container.privileged=true`

```bash
echo "ryuk.container.privileged=true" >> ~/.testcontainers.properties
```

```bash
podman machine set --rootful
podman machine start
```

Finally, on ARM, an [extra step](https://edofic.com/posts/2021-09-12-podman-m1-amd64) is required to make AMD64 images work:

```bash
podman machine ssh
sudo -i
rpm-ostree install qemu-user-static
systemctl reboot
```

Now remove administrator privileges from the `githubactions` user.
As with the user creation, it's usually easiest to do this in the GUI.

### Rosetta

A fresh install of macOS will not have Rosetta installed, so Intel binaries cannot run. Fix this by running:

```bash
softwareupdate --install-rosetta --agree-to-license
```

### Stand up the actions runner

#### Download the runner scripts

GitHub provides an installation package of customized runner scripts for each repository.
Follow [the instructions](https://docs.github.com/en/actions/hosting-your-own-runners/adding-self-hosted-runners)
to install the scripts for the repository to be built.

Choose the default group, a descriptive name, and the default work folder. Add a label `macos-arm64-latest`.
If you forget the label, it will need to be added [through the UI](https://docs.github.com/en/actions/hosting-your-own-runners/using-labels-with-self-hosted-runners).

#### Cleanup and setup logic

Self-hosted runners do not run on ephemeral hardware, and so workflow runs may need to clean up.
We also need to start a podman machine before the job if one is not already running.

To create cleanup and setup scripts and hooks, run:

```bash
echo "rm -rf /Users/githubactions/actions-runner/_work" > /Users/githubactions/runner-cleanup.sh
echo "podman machine info | grep Running || podman machine start" >> /Users/githubactions/podman-start.sh
chmod a+x /Users/githubactions/runner-cleanup.sh
echo ACTIONS_RUNNER_HOOK_JOB_COMPLETED=/Users/githubactions/runner-cleanup.sh >> .env
echo ACTIONS_RUNNER_HOOK_JOB_STARTED=/Users/githubactions/podman-start.sh >> .env
```

In the same script, we also ensure that the podman machine is running before jobs execute.

#### Start the runner

To test the runner, run

`run.sh`

Once you're happy the runner is processing builds correctly, it's time to create it as a daemon.

### Start the service on reboot

Note that GitHub have scripts for this, but theirs run as LaunchAgents, not LaunchDaemons.
LaunchAgents run when a user logs in, and LaunchDaemons run on boot, so the daemon option seems preferable.

As administrator, `sudo vi /Library/LaunchDaemons/actions.runner.quarkusio-quarkus.macstadium-m1.plist`.

Then add the following:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>actions.runner.quarkusio-quarkus.macstadium-m1</string>

    <key>RunAtLoad</key>
    <true/>

    <key>KeepAlive</key>
    <true/>
  
    <key>UserName</key>
    <string>githubactions</string>

    <key>ProgramArguments</key>
    <array>
     <string>/Users/githubactions/actions-runner/runsvc.sh</string>
    </array>

    <key>WorkingDirectory</key>
    <string>/Users/githubactions/actions-runner</string>

    <key>StandardOutPath</key>
    <string>/tmp/github.runner.plist.stdout</string>
    <key>StandardErrorPath</key>
    <string>/tmp/github.runner.plist.stderr</string>

    <key>EnvironmentVariables</key>
    <dict>
      <key>ACTIONS_RUNNER_SVC</key>
      <string>1</string>
    </dict>
    <key>ProcessType</key>
    <string>Interactive</string>
    <key>SessionCreate</key>
    <true/>
  </dict>
</plist>
```

Finally, run

```shell
sudo launchctl load -w /Library/LaunchDaemons/actions.runner.quarkusio-quarkus.macstadium-m1.plist
```

You can check the logs in `/tmp/github.runner.plist.stdout` to confirm everything is working.
