# CharacterRecognition
Handwritten alhpanumeric character recognition using keras (tensorflow 2).

### Data sets

For this project, we will use the [EMNIST](https://arxiv.org/abs/1702.05373v1) Data set. A csv format of the data set can be found on the [kaggle page](https://www.kaggle.com/crawford/emnist)
We will use the digits and capitals letters from this dataset as this will be the characters found on a plate.

We will also create a second dataset, much smaller, for fine tuning. It will be generated with license plate fonte.


### Model

The CNN model is trained in the 'Model_training.ipynb' notebook. There is a saved model in the folder 'Models' that achieved 98.3% test accuracy.
