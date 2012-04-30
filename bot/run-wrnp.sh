#!/bin/bash

java -jar bin/bbbot.jar --server http://lb.mconf.org:8080 --key wrnp2 --meeting "Demo Meeting" --video etc/video-sample-small.flv --audio etc/audio-sample.flv --numbots 100 --interval 0 --probabilities "2:58.68;3:20.82;4:10.14;5:4.56;6:2.74;7:1.21;8:0.8;9:1.05"
