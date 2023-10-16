# Reclaim disk space, otherwise we have too little free space at the start of a job
#
# Numbers as of 2022-01-26:
#
# $ df -h
# Filesystem      Size  Used Avail Use% Mounted on
# /dev/root        84G   52G   32G  63% /
# devtmpfs        3.4G     0  3.4G   0% /dev
# tmpfs           3.4G  4.0K  3.4G   1% /dev/shm
# tmpfs           696M  1.1M  695M   1% /run
# tmpfs           5.0M     0  5.0M   0% /run/lock
# tmpfs           3.4G     0  3.4G   0% /sys/fs/cgroup
# /dev/loop0       62M   62M     0 100% /snap/core20/1270
# /dev/sda15      105M  5.2M  100M   5% /boot/efi
# /dev/loop1       68M   68M     0 100% /snap/lxd/21835
# /dev/loop2       44M   44M     0 100% /snap/snapd/14295
# /dev/sdb1        14G  4.1G  9.0G  32% /mnt
#
# $ docker images
# REPOSITORY       TAG         IMAGE ID       CREATED        SIZE
# node             12-alpine   8a6e486e9817   2 weeks ago    91.1MB
# node             16-alpine   23990429c0d7   2 weeks ago    109MB
# node             12          44d575d74d9f   2 weeks ago    918MB
# node             14          24d97ba03bf7   2 weeks ago    944MB
# node             14-alpine   194cd0d85d8a   2 weeks ago    118MB
# node             16          842962c4b3a7   2 weeks ago    905MB
# ubuntu           20.04       d13c942271d6   2 weeks ago    72.8MB
# ubuntu           18.04       886eca19e611   2 weeks ago    63.1MB
# buildpack-deps   stretch     46000751048f   5 weeks ago    835MB
# buildpack-deps   buster      ac4279e940f3   5 weeks ago    804MB
# buildpack-deps   bullseye    d724319bd076   5 weeks ago    834MB
# debian           9           c599fc96ef79   5 weeks ago    101MB
# debian           10          8a94f77c4ac3   5 weeks ago    114MB
# debian           11          6f4986d78878   5 weeks ago    124MB
# moby/buildkit    latest      19340e24de14   2 months ago   144MB
# alpine           3.12        b0925e081921   2 months ago   5.59MB
# alpine           3.13        6b7b3256dabe   2 months ago   5.62MB
# alpine           3.14        0a97eee8041e   2 months ago   5.6MB
# ubuntu           16.04       b6f507652425   4 months ago   135MB

time sudo docker image prune --all --force || true
# That is 979M
time sudo rm -rf /usr/share/dotnet
# That is 1.78 GB
time sudo rm -rf /usr/share/swift
