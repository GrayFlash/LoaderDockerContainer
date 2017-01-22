# Loader Container [![Build Status](https://travis-ci.org/camicroscope/LoaderDockerContainer.svg?branch=master)](https://travis-ci.org/camicroscope/LoaderDockerContainer)

Contains imageLoader and uAIMDataLoader for loading images and markups into caMicroscope.

# Building

`docker build -t camicroscope_loader .`

# Running

```

mkdir img
export CAMIC_IMAGES_DIR=$(echo $(pwd)/img)

export CAMIC_KUE_PORT=6000
export CAMIC_MARKUPLOADER_PORT=6001
export CAMIC_DATALOADER_PORT=6002

export dataloader_host=127.0.0.1 #IP address of data container
export annotations_host=127.0.0.1 #IP address of data container
export mongo_host=127.0.0.1 #IP address of data container
export mongo_port=27017
```

```
docker run -itd -p $CAMIC_KUE_PORT:3000 -p $CAMIC_MARKUPLOADER_PORT:3001 -p $CAMIC_DATALOADER_PORT:3002 -v $CAMIC_IMAGES_DIR:/data/images -e "dataloader_host=$(echo $dataloader_host)" -e "annotations_host=$(echo $annotations_host)" -e "mongo_host=$(echo $mongo_host)" -e "mongo_port=$(echo $mongo_port)"  camicroscope_loader
```



## Dataloader API
Following examples assume `<API_PORT>==32799`

### POST /submitData
`curl -v -F id=TCGA-02-0001 -F upload=@TCGA-02-0001-01Z-00-DX1.83fce43e-42ac-4dcd-b156-2908e75f2e47.svs http://localhost:32799/submitData`
Return type: `json`
On success returns: `{"status":"success"}`

### GET /subjectIdExists
`curl localhost:32799/subjectIdExists?SubjectUID=TCGA-02-0001`
Returns an array with subjectID and file path.
Empty array if subject Id doesnt exist

### GET /getMD5ForImage
`curl localhost:32799/getMD5ForImage?imageFileName=TCGA-02-0001-01Z-00-DX1.83fce43e-42ac-4dcd-b156-2908e75f2e47.svs`
Returns an array with MD5 of the image
`[{"md5sum":"418a0724b0a2113bcd2956bacae105b7"}]`
