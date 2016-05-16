#!/bin/bash
execstack -c /root/OpenCV/opencv-3.0.0/build/lib/libopencv_java300.so &
service redis-server start

# Run Image Loader
nohup nodejs /root/dataloader/bin/www &

# Run Annotations Loader
nohup nodejs /root/annotationloader/bin/www &
# Run KUE Dashboard
nohup nodejs /root/annotationloader/node_modules/kue/bin/kue-dashboard & 
while true; do sleep 1000; done

