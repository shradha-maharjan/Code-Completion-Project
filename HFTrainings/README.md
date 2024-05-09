**This repository contains code for training a causal language model(causal-model-a.py), training a causal language model along with tokenizer training(casual-model-token.py) and training a masked language model(mask-model_training.py).**

The code snippets are mostly taken from Hugging face's "Training a causal language model from scratch" and  "Fine-tuning a masked language model". The model is trained on the CodeSearchNet dataset, specifically focusing on the Java programming language. The trained model can generate code snippets based on input prompts.

**Requirements**
- Python 3.x
- PyTorch
- Transformers
- Hugging Face Datasets
- Accelerate
- tqdm
- huggingface_hub

**Setup**
Install the required Python packages by running pip install -r requirements.txt.
Ensure you have the necessary CUDA drivers and hardware for GPU acceleration if you intend to train the model on a GPU.

**Training a causal language model(causal-model-a.py):**

Usage:
Run the provided Python script causal-model-a.py to start training the model.

Dataset Preparation: The CodeSearchNet dataset is loaded and preprocessed for training. It includes splitting the dataset into train, test, and validation sets.

Tokenization: AutoTokenizer from the Hugging Face Transformers library is used to tokenize the code snippets. We have used huggingface-course/code-search-net-tokenizer as tokenizer.

Model Configuration: The model configuration is set up using AutoConfig from Hugging Face Transformers. The GPT-2 large model architecture is utilized, customized with specific configuration parameters for the Java code domain.

Model Training: The GPT-2 language model is initialized and trained on the preprocessed dataset. Training is accelerated using the Accelerate library to leverage multiple GPUs efficiently.

Evaluation Metrics: Various evaluation metrics such as entropy, loss, perplexity, BLEU score, and ROUGE score are computed to assess the quality and performance of the trained model.

Entropy: Entropy measures the uncertainty or randomness in the distribution of predicted probabilities. In the context of language modeling, it quantifies how predictable the model's output is. Lower entropy values indicate higher confidence in the model's predictions and less uncertainty.

Loss: Loss represents the discrepancy between the model's predictions and the actual ground truth labels. Cross-entropy loss, a commonly used metric in language modeling tasks, is employed in this code to calculate the loss. Lower loss values indicate better alignment between the model's predictions and the ground truth.

Perplexity: Perplexity serves as a measure of how well a probability distribution or language model predicts a sample. It is computed as the exponentiation of the cross-entropy loss. Lower perplexity values signify that the model assigns higher probabilities to the correct tokens in the sequence, reflecting superior predictive capability.

Then, the generated code snippets are evaluated using BLEU and ROUGE metrics to assess their similarity to ground truth code snippets.

BLEU Score: BLEU is a metric commonly used to evaluate the quality of machine-translated text. It compares the generated text against one or more reference texts and computes a score based on the n-gram overlap between the generated text and the reference texts. Higher BLEU scores indicate better agreement between the generated and reference texts, with a perfect score of 1.0 representing identical output.

ROUGE Score: ROUGE is primarily used for evaluating text summarization and summarization-related tasks. It assesses the overlap between the generated summary and the reference summaries by considering various factors such as unigram, bigram, and longest common subsequences. Higher ROUGE scores indicate better agreement between the generated summary and the reference summaries.

Inference: Once trained, the model can be used for code generation tasks. A pipeline is set up for text generation using the trained model checkpoint.

Credits:
This project utilizes several open-source libraries and frameworks, including:

- Hugging Face Transformers: For tokenization, model configuration, and training utilities.
- Accelerate: For efficient distributed training across multiple GPUs. The code utilizes the Accelerate library to train the causal language model efficiently 
- CodeSearchNet Dataset: The dataset used for training the language model.


**Training a causal language model along with tokenizer training(casual-model-token.py):**

Everything is similar except the part that a base tokenizer is trained on the dataset to generate a vocabulary tailored to the Java programming language.

Usage:
Run the provided Python script causal-model-token.py to start training the model.

**Masked Language Model Training (MLM_wholewordmask_train.py):**

This Python script trains and evaluates a masked language model using the CodeBERT model. It leverages the Hugging Face Transformers library for model loading, tokenization, and training. The script uses Hugging face's Trainer API.

Usage:
python your_script.py -train <train_size> -test <test_size>

Replace <train_size>, <test_size> with the desired sizes for the training, validation, and test datasets, respectively.

Description of the Script
Argument Parsing: The script uses the argparse module to parse command-line arguments for specifying the sizes of the training, validation, and test datasets.

Dataset Loading: It loads the Java datasets from the CodeSearchNet repository using the load_dataset function from the datasets library. The datasets are split into training, validation, and test sets according to the specified sizes.

Tokenization: The script tokenizes the dataset using the CodeBERT model's tokenizer obtained from the Hugging Face model hub.

Data Preparation: It prepares the dataset by concatenating sequences into chunks and creating a labels column for the masked language modeling task.

Model Initialization: The pretrained CodeBERT model for masked language modeling is loaded using the AutoModelForMaskedLM class from the Hugging Face library.

Training Loop: The script trains the model for a specified number of epochs using the training dataset. It utilizes an AdamW optimizer and a linear learning rate scheduler. The training progress is monitored using a progress bar.

Evaluation: After each epoch, the model's performance is evaluated on the test dataset. The loss, perplexity, and entropy metrics are calculated and printed.

**MRR (MLM_wholewordmask_eval.py)**
Masked Language Model Prediction: The script uses the trained model to predict the masked token in a sample text. It calculates the Mean Reciprocal Rank (MRR) based on the scores obtained from the model predictions.

Results Analysis: The MRR is computed and printed to evaluate the model's performance.