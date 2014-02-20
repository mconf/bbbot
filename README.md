BBBot
=====

BBBot is a command line client for [BigBlueButton](http://mconf.org) developed in the project [Mconf](http://mconf.org). It can connect several _bots_ to a conference and do things like send and receive audio and video.

It is a java application based on the libraries `bbb-java` and `flazr` that were originally built by Mconf to be used in `BBB-Android`, an Android client for BigBlueButton.


Usage
-----
Java version required:

    java version "1.7.0_21"

To use the bot run the jar file at `bot/bin/bbbot.jar`. See its options with:

    java -jar bot/bin/bbbot.jar --help

Command line examples:

1 bot sending audio and video on "Demo Meeting"

    java -jar bin/bbbot.jar --meeting "Demo Meeting" --numbots 1 --server http://my-bbb-server --key key_value --audio "etc/audio-sample.flv" --video "etc/video-sample.flv"
    

5 bots on a single meeting sending audio and video on "Demo Meeting"

    java -jar bin/bbbot.jar --meeting "Demo Meeting" --numbots 5 --server http://my-bbb-server --key key_value --single_meeting true --video "etc/video-sample.flv"  --audio "etc/audio-sample.flv" --everyone_sends_video true



Development
-----------

Clone this repository, download the submodules and use ant to build the bot:

    git submodule init
    git submodule update
    cd bot/
    ant dist


Development Tips
----------------

### Creating flv files with speex audio

Download WAV files, that will be converted to SPX (Speex format) using `speexenc` and then encapsulated in an FLV using `ffmpeg`.

First, install `xuggle-ffmpeg` that has libspeex enabled and will also install `speexenc`: http://build.xuggle.com/job/xuggler_jdk5_stable/

* Use `xuggle-ffmpeg.4.0.1084-i686-pc-linux-gnu.sh` in Ubuntu 32 bits.
* The installer will output some lines to be included in your `~/.profile`. After adding them, run `source ~/.profile` to load it.

The WAV files need to be PCM encoded, with 1 channel and sample rate of 16000 Hz. To check your file run:

    ffmpeg -i input.wav
    > Audio: pcm_s16le, 16000 Hz, 1 channels, s16, 256 kb/s

You can also convert a WAV to 16kHz and 1 channel with:

    ffmpeg -i input.wav -ac 1 -ar 16000 output.wav

Converting WAV to SPX:

    speexenc --quality 6 --comp 10 --nframes 1 input.wav output.spx

Encapsulating SPX in an FLV:

    ffmpeg -i input.spx -y -vn -acodec copy output.flv


To see the quality of encoding that BigBlueButton uses for Speex see: https://github.com/bigbluebutton/bigbluebutton/blob/master/bigbluebutton-client/src/org/bigbluebutton/modules/phone/managers/StreamManager.as#L92


More about Mconf
----------------

This project is developed as a part of Mconf. See more about Mconf at:

* [Mconf website](http://mconf.org)
* [Mconf @ Google Code](http://code.google.com/p/mconf)
* [Mconf @ GitHub](https://github.com/mconf)
