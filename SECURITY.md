<!-- content also visible in quarkusio/security
     copy changes there too -->

The canonical version of this document is hosted on the Quarkus website at [Quarkus security](https://quarkus.io/security/).

# Security policy

The Quarkus team and community take all security bugs very seriously.
You can find our guidelines here regarding our policy and security disclosure.

## Reporting security issues

:warning: Do NOT report security issues in our public bug tracker.

Please report any security issues you find in Quarkus to:

    security at quarkus.io

Anyone can post to this list. The subscribers are only trusted individuals from the Quarkus community who will handle the resolution of any reported security issues in confidence. In your report, please note how you would like to be credited for discovering the issue and the details of any embargo you would like to impose. Currently, the security response teams for the following distributions are subscribed to this list and will respond to your report:

* [Red Hat](https://access.redhat.com/security/team/contact/)

### Ecosystem

Quarkus is an ecosystem made from many extensions and many libraries (like Eclipse Vert.x, Hibernate, Apache Camel and more), most of them not under the direct responsibility of the Quarkus team.
If you find a security bug possibly rooted in one of these libraries, you can either disclose to them directly or disclose them to the Quarkus team (following this process) and we will responsibly disclose the issue to the respective extension or library maintainer.

### Why follow this process

Due to the sensitive nature of security bugs, the disclosure process is more constrained than a regular bug.
We appreciate you following these industry accepted guidelines, which gives time for a proper fix and limit the time window of attack.

## Supported Versions

The community will fix security bugs for the latest major.minor version published at <https://quarkus.io/get-started/>.

| Version      | Supported          |
| ------------ | ------------------ |
| Latest 3.x   | :white_check_mark: |
| 3.20 LTS     | :white_check_mark: |
| 3.15 LTS     | :white_check_mark: |
| 3.8 LTS      | :white_check_mark: |
| Older 3.x    | :x:                |
| < 3          | :x:                |

We may fix the vulnerability to older versions depending on the severity of the issue and the age of the release, but we are only committing to the versions documented above.

## Handling security issues

If you represent a Quarkus extension or a Quarkus platform, you are welcome to subscribe to the security at quarkus.io mailing list. Your subscription will only be approved if you can demonstrate that you will handle issues in confidence and properly credit reporters for discovering issues (e.g. experience with embargo process).
