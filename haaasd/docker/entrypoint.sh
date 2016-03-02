#!/bin/sh

# Create users
for APP in OCE ECE MPD PAO;do
    for PLT in r u e p h;do
        for N in $(seq 1 10);do
            useradd $(echo hap$APP$PLT$N | tr '[:upper:]' '[:lower:]')
        done
    done
done

export HAP_INTERACTIVE=0

/haaasd --config /haaasd.conf -ip $(hostname -i)
