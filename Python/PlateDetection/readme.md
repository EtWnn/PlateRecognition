# Detection Model

The following aims at : 
	- describing how to build and train the detection model
	- explaining how to convert this model in TFlite in order to export it in Android

## Environment installation

In your environment, dowload the necessary packages. We used Tensorflow 1.15 (CPU version) for the training.
```
pip install -r /path/to/requirements.txt
```

## TensorFlow Object Detection API Installation

To build and train the model, we used the TensorFlow Object Detection API Framework.

The TensorFlow Object Detection API is an open source framework built on top of TensorFlow that facilitates the construction and the training of an object detection model.

Download the model from the associated github repository : [https://github.com/tensorflow/models/tree/master](https://github.com/tensorflow/models/tree/master)

Rename the folder models-master in models. 
At this stage, your folder should look like that : 
```
plate_detection
└─ requirements
└─ models
    ├── official
    ├── research
    ├── samples
    └── tutorials
```
Download the protobuf library
```
$ cd models/research/
$ protoc object_detection/protos/*.proto --python_out=.
```
Dowload the necessary object detection api specific packages. From  TensorFlow/models/research/ :
```
$ python setup.py build
$ python setup.py install
```
Make sure you have Visual C++ 2015 installed and on your path.  Then run the following command to install the COCO API that will be usefull for the training :
Fine tune the detection model with the current tfrecord files:
```
pip install git+https://github.com/philferriere/cocoapi.git#subdirectory=PythonAPI
```
## Training


Create a new folder named "workspace" that will contain the pre-trained model, the images and record files, the config files and the fine-tunned final models.

plate_detection
└─ requirements.txt
└─ models
    ├── official
    ├── research
    ├── samples
    └── tutorials
└─ workspace
    ├── pre-trained-model
    ├── images
    ├── training
    └── annotations
### Image Labelling

Put all the images you want to train the model with in an images folder.
Install and launch LabelImg in order to label the image.

```
pip3 install labelImg
labelImg
```
Once labelImg is launched, select your folder image and start labelling your images. It will create for each image an xml file.

```
$ python separate_train_test.py -x -i training_demo\images -r 0.1
```
This will create two folders train and test in the image folder.


### Conversion in .record files

Once the XLM files are created, we need to convert them in record files to train the model. 

We furst need to produce a csv file first :


for the training dataset
```
python xml_to_csv.py -i [PATH_TO_IMAGES_FOLDER]/train -o [PATH_TO_ANNOTATIONS_FOLDER]/train_labels.csv
```
for the test dataset
```
python xml_to_csv.py -i [PATH_TO_IMAGES_FOLDER]/test -o [PATH_TO_ANNOTATIONS_FOLDER]/test_labels.csv
```
Once the csv files are produced, we can convert it in record files.
Create train data

```
python generate_tfrecord.py --label=<LABEL> --csv_input=<PATH_TO_ANNOTATIONS_FOLDER>/train_labels.csv
--img_path=<PATH_TO_IMAGES_FOLDER>/train  --output_path=<PATH_TO_ANNOTATIONS_FOLDER>/train.record
```
Create test data :
```
python generate_tfrecord.py --label=<LABEL> --csv_input=<PATH_TO_ANNOTATIONS_FOLDER>/test_labels.csv
--img_path=<PATH_TO_IMAGES_FOLDER>/test
--output_path=<PATH_TO_ANNOTATIONS_FOLDER>/test.record
```

## Configure the training

### Creating label map

create a label_map.pbtxt with the following in the annotations folder
```
item {
    id: 1
    name: 'cat'
}
```


### Pre-trained-model

We will not create a trained model from scratch but we will start from a pre-trained model that we will fine-tune. For the pre-trained model we chosed the [ssd_mobilenet_v3_large_coco](http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v3_large_coco_2019_08_14.tar.gz) 

Once downloaded, extract its content in the pre-trained model folder. 
To configure the pipeline, you need to modify and update the pipeline.config file. The necessary modifications are described in the config.file itself.

### Training the model

```
python model_main.py --alsologtostderr --model_dir=training/ --pipeline_config_path=training/pre-trained-model.config
```
Once the model is trained you should have mutiple ckpt file in the training folder, i.e the model


## Conversion to tflite

Once the model is trained, we need to convert it in a tflite file.

### Conversion to a frozen graph compatible with tflite 


```
python export_tflite_ssd_graph.py --pipeline_config_path="training/pipeline.config" --trained_checkpoint_prefix="training/model.ckpt-xxx" --output_directory="tflite" -- add_postprocessing_op=true
```

### Conversion to a tflite


```
tflite_convert --graph_def_file=tflite2/tflite_graph.pb --output_file=tflite/detect.tflite --output_format=TFLITE --input_shapes=1,320,320,3 --input_arrays=normalized_input_image_tensor --output_arrays='TFLite_Detection_PostProcess','TFLite_Detection_PostProcess:1','TFLite_Detection_PostProcess:2','TFLite_Detection_PostProcess:3' --inference_type=FLOAT--mean_values=128 --std_dev_values=127 --change_concat_input_ranges=false --allow_custom_ops
```

This will create the detect.tflite that you will be able to transfer to the android app.

`