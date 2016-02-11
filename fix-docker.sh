#!/usr/bin/env bash
VBoxManage modifyvm default --natdnshostresolver1 on
VBoxManage modifyvm default --natdnsproxy1 on