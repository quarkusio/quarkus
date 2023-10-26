# this script might be useful to get some insights about the disk usage
# obviously, it needs to be tuned adjusted

# if you can actually write something to the disk and execute an action,
# using ncdu to dump an analysis of the disk and upload it as an artifact
# might be a better option that this adhoc script

echo "# df -h"
df -h
echo "# du -sh /"
sudo du -sh /* || true
echo "# du -sh /home/runner/work/quarkus/quarkus/integration-tests/*"
sudo du -sh /home/runner/work/quarkus/quarkus/integration-tests/* || true
echo "# docker images"
docker images || true
echo "# du -sh /var/lib/*"
sudo du -sh /var/lib/* || true
echo "# du -sh /opt/hostedtoolcache/*"
sudo du -sh /opt/hostedtoolcache/* || true
echo "# du -sh /imagegeneration/installers/*"
sudo du -sh /imagegeneration/installers/* || true
