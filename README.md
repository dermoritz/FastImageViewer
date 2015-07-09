# FastImageViewer

The only purpose of this program is to **browse through pictures - fast**. Today most pictures resolution is much higher than monitor's resolution. Because of this i also need a fast zoom to 100%. This viewer will zoom to 100% on left click to cursor's position. 
The only software i know that works the same way is [Fastpictureviewer](java.util.ConcurrentModificationException) - the best image browser imho.
"Fastimageviewer" is trying to create a free alternative on the one hand and it let me try JavaFX/Java8 on the other hand.

## Build

Until now this is only a prototype with no distributed binaries. But creating the binary is simple: 

``mvn clean package`` creates an executable jar.

## First working prototype

Within 36h a first working prototype (0.0.1) was created. It takes one argument (folder or image) and you can browse through images in this folder. As mentioned above you can zoom with left click. It will prefetch the next 2 images and it holds the last in memory. If you start through console you will see much logging.
