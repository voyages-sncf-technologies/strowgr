#!/bin/bash
MACHINE=default
VBoxManage="/c/Program Files/Oracle/VirtualBox/VBoxManage.exe"

if [ "$1" = "netfix" ];then
    docker-machine stop $MACHINE
    "$VBoxManage" modifyvm $MACHINE --natdnshostresolver1 on
    "$VBoxManage" modifyvm $MACHINE --natdnsproxy1 on
    docker-machine start $MACHINE
    echo "Wait 2 seconds ..."
    sleep 2
fi

do_mount(){
    docker-machine ssh $MACHINE "sudo mkdir -p $2 &&\
        cat /proc/mounts | grep \"$1 $2 \" || sudo mount.vboxsf $1 $2 &&\
        grep \"$1   $2 \" /etc/fstab || sudo su root -c 'echo \"$1   $2   vboxsf   defaults  0   0\" >> /etc/fstab'"
}

"$VBoxManage" sharedfolder add $MACHINE --name "D_DRIVE" --hostpath "D:\\"
do_mount "D_DRIVE" "/d"
do_mount "D_DRIVE" "/cygdrive/d"
