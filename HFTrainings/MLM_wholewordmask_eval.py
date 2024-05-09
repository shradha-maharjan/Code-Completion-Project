
from transformers import pipeline
import pandas as pd
import logging
from prettytable import PrettyTable 

logging.basicConfig(filename='masked_text.log', level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger()

model = "MLM_FinetunedModel"
pred_model = pipeline("fill-mask", model="shradha01/MLM_FinetunedModel_accel")
text = "public class HelloWorld {\n\tpublic static void main(String[] args) { \n\t\tSystem.out.<mask>('Hello,World!'); \n} \n}"

# Get predictions
preds = pred_model(text)
# Path to the file containing masked texts and ground truth texts
masked_texts_file = "/home/user1-selab3/shradha_test/jsoninput/outputs.txt"
ground_truth_file = "/home/user1-selab3/shradha_test/jsoninput/output_java.txt"

# List to store reciprocal ranks for each masked text
reciprocal_ranks = []

# Read the masked texts and ground truth texts from their respective files
with open(masked_texts_file, "r") as masked_file, open(ground_truth_file, "r") as truth_file:
    masked_texts = masked_file.readlines()
    ground_truth_texts = truth_file.readlines()
    
# Initialize an empty list to store predictions
all_preds = []

# Initialize a counter for masked token IDs
masked_token_id_counter = 0

# Initialize a counter for ground truth text line IDs
ground_truth_line_id_counter = 0

# Initialize an empty list to store ground truth texts and their IDs
ground_truth_data = []

# Iterate through masked texts and ground truth texts
for masked_text, truth_text in zip(masked_texts, ground_truth_texts):
    # Increment the masked token ID counter for each new masked token
    masked_token_id_counter += 1
    
    # Get predictions for the current masked text
    preds = pred_model(masked_text, top_k= 10)
    
    # Initialize an empty list to store predictions for the current masked text
    masked_text_preds = []

    # Iterate through predictions for the current masked text
    for rank, pred in enumerate(sorted(preds, key=lambda x: x['score'], reverse=True), start=1):
        # Create a dictionary for each prediction with required fields
        pred_dict = {
            'token_id': masked_token_id_counter,
            'rank': rank,
            'score': pred['score'],
            'token': pred['token'],
            'token_str': pred['token_str'],
            'sequence': pred['sequence']
        }
        # Append the prediction dictionary to the list of predictions for the current masked text
        masked_text_preds.append(pred_dict)

    # Append the list of predictions for the current masked text to the list of all predictions
    all_preds.extend(masked_text_preds)

    # Increment the ground truth line ID counter
    ground_truth_line_id_counter += 1

    # Store ground truth text and its ID
    ground_truth_data.append({'ground_truth_text': truth_text, 'token_id': ground_truth_line_id_counter})

    # Log ground truth text, input, and predicted results
    logger.info(f"-----\nSample {ground_truth_line_id_counter}\nGround truth:\n{truth_text.strip()}\nInput:\n{masked_text.strip()}\nPredicted results (sorted by probability scores):")
    for result in masked_text_preds:
        logger.info(f"   Result {result['rank']}: {result['token_str']}, {result['score']:.4f}, {result['sequence']}")

# Convert the list of predictions into a DataFrame
preds_df = pd.DataFrame(all_preds)

# Convert the list of ground truth data into a DataFrame
ground_truth_df = pd.DataFrame(ground_truth_data)

results = preds_df.merge(ground_truth_df, how='left', on=['token_id'])

def fill_sequence(row):
    if row['sequence'] in row['ground_truth_text']:
        return row['sequence']
    else:
        return None

# Apply the function to the 'sequence' column
results['match_sequence'] = results.apply(fill_sequence, axis=1)

# Display the results DataFrame
logger.info(results['match_sequence'])

results['match_sequence'].fillna('None', inplace=True)

# Group by 'token_id' and 'match_sequence', taking the minimum rank
relevances_rank = results.groupby(['token_id', 'match_sequence'])['rank'].min()

logger.info(relevances_rank)

ranks = relevances_rank[relevances_rank.index.get_level_values('match_sequence') != 'None']

logger.info(ranks)

reciprocal_ranks = 1 / (ranks)
reciprocal_ranks

mean_reciprocal_rank = reciprocal_ranks.mean()


logger.info(f"Mean Reciprocal Rank: {mean_reciprocal_rank:.2f}")