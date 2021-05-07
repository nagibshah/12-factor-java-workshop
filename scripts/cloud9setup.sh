#!/bin/bash

sudo yum -y update
sudo yum -y install libunwind
sudo apt -y update
pip install awscli --upgrade --user
pip install aws-sam-cli --upgrade --user
ln -sfn $(which sam) ~/.c9/bin/sam
curl -s -L https://dot.net/v1/dotnet-install.sh -O
sudo chmod u=rx dotnet-install.sh
./dotnet-install.sh -c Current
echo "export PATH=$PATH:$HOME/.local/bin:$HOME/bin:$HOME/.dotnet:$HOME/.dotnet/tools" >> ~/.bashrc
echo "export DOTNET_ROOT=$HOME/.dotnet" >> ~/.bashrc
. ~/.bashrc
dotnet tool install -g Amazon.Lambda.Tools
rm dotnet-install.sh
