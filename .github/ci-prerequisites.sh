# Reclaim disk space, otherwise we have too little free space at the start of a job
#
# Numbers as of 2023-01-06:
#
# $ df -h
# Filesystem      Size  Used Avail Use% Mounted on
# /dev/root        84G   48G   36G  58% /
# tmpfs           3.4G  172K  3.4G   1% /dev/shm
# tmpfs           1.4G  1.1M  1.4G   1% /run
# tmpfs           5.0M     0  5.0M   0% /run/lock
# /dev/sda15      105M  5.3M  100M   5% /boot/efi
# /dev/sdb1        14G  4.1G  9.0G  31% /mnt
# tmpfs           695M   12K  695M   1% /run/user/1001
#
# $ docker images
# REPOSITORY       TAG         IMAGE ID       CREATED        SIZE
# node             14-alpine   b4fb2cece133   3 weeks ago    123MB
# node             16-alpine   bb97fd22e6f8   3 weeks ago    118MB
# node             18-alpine   6d7b7852bcd3   3 weeks ago    169MB
# ubuntu           22.04       6b7dfa7e8fdb   3 weeks ago    77.8MB
# ubuntu           20.04       d5447fc01ae6   3 weeks ago    72.8MB
# ubuntu           18.04       251b86c83674   3 weeks ago    63.1MB
# node             14          c08c80352dd3   4 weeks ago    915MB
# node             16          993a4cf9c1e8   4 weeks ago    910MB
# node             18          209311a7c0e2   4 weeks ago    991MB
# buildpack-deps   buster      623b2dda3870   4 weeks ago    803MB
# buildpack-deps   bullseye    8cbf14941d59   4 weeks ago    835MB
# debian           10          528ac3ebe420   4 weeks ago    114MB
# debian           11          291bf168077c   4 weeks ago    124MB
# alpine           3.16        bfe296a52501   7 weeks ago    5.54MB
# moby/buildkit    latest      383075513bdc   8 weeks ago    142MB
# alpine           3.14        dd53f409bf0b   4 months ago   5.6MB
# alpine           3.15        c4fc93816858   4 months ago   5.58MB

time sudo docker image prune --all --force || true
# That is 979M
time sudo rm -rf /usr/share/dotnet || true
# That is 1.7G
time sudo rm -rf /usr/share/swift || true
# Remove Android
time sudo rm -rf /usr/local/lib/android || true
# Remove Haskell
time sudo rm -rf /opt/ghc || true
time sudo rm -rf /usr/local/.ghcup || true
# Remove pipx
time sudo rm -rf /opt/pipx || true
# Remove Rust
time sudo rm -rf /usr/share/rust || true
# Remove Go
time sudo rm -rf /usr/local/go || true
# Remove miniconda
time sudo rm -rf /usr/share/miniconda || true
# Remove powershell
time sudo rm -rf /usr/local/share/powershell || true
# Remove Google Cloud SDK
time sudo rm -rf /usr/lib/google-cloud-sdk || true

# Remove infrastructure things that are unused and take a lot of space
time sudo rm -rf /opt/hostedtoolcache/CodeQL || true
time sudo rm -rf /imagegeneration/installers/go-* || true
time sudo rm -rf /imagegeneration/installers/node-* || true
time sudo rm -rf /imagegeneration/installers/python-* || true
