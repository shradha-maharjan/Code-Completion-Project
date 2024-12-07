import logging

log_file_path = "validation_warnings.log"
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler(log_file_path, mode='w', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class DatasetValidator:
    def __init__(self, original_file_path, masked_file_path, mask_token="[MSK]"):
        self.original_data = self.load_file(original_file_path)
        self.masked_data = self.load_file(masked_file_path)
        self.mask_token = mask_token

    @staticmethod
    def load_file(file_path):
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                return [line.strip() for line in file.readlines()]
        except FileNotFoundError:
            logger.error(f"File not found: {file_path}")
            raise
        except Exception as e:
            logger.error(f"Error loading file {file_path}: {e}")
            raise

    def validate_reconstruction(self):
        
        for index, (original_line, masked_line) in enumerate(zip(self.original_data, self.masked_data)):
            original_tokens = original_line.split()
            masked_tokens = masked_line.split()

            # Step 1: Find indices of masked tokens
            mask_indices = [i for i, token in enumerate(masked_tokens) if token == self.mask_token]

            # Step 2: Check for mismatched lengths
            if len(mask_indices) > len(original_tokens) or any(i >= len(original_tokens) for i in mask_indices):
                logger.warning(f"Skipping problematic line {index + 1}:")
                logger.warning(f"Original: {' '.join(original_tokens)}")
                logger.warning(f"Masked: {' '.join(masked_tokens)}")
                continue  # Skip this line and move to the next

            # Step 3: Extract original tokens
            original_tokens_at_indices = [original_tokens[i] for i in mask_indices]

            # Step 4: Reconstruct dataset
            reconstructed_tokens = masked_tokens.copy()
            for i, mask_index in enumerate(mask_indices):
                reconstructed_tokens[mask_index] = original_tokens_at_indices[i]

            # Step 5: Compare original and reconstructed tokens
            if original_tokens != reconstructed_tokens:
                logger.warning(f"Mismatch at line {index + 1}:")
                logger.warning(f"Original: {' '.join(original_tokens)}")
                logger.warning(f"Masked: {' '.join(masked_tokens)}")
                logger.warning(f"Reconstructed: {' '.join(reconstructed_tokens)}")
            else:
                logger.info(f"Line {index + 1}: Reconstruction successful.")
                logger.warning(f"Original code: {' '.join(original_tokens)}")
                logger.warning(f"Masked code: {' '.join(masked_tokens)}")
                logger.warning(f"Masked tokens: {' '.join(original_tokens_at_indices)}")
                logger.warning(f"Reconstructed: {' '.join(reconstructed_tokens)}")

if __name__ == "__main__":
    original_file_path = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/pretrain_dataset_2ndVersion/Tokenized_Code/pretrain_tokenized_gen.txt"
    masked_file_path = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/pretrain_dataset_2ndVersion/Masked_tokenized/pretrain-fun-masked-gen.txt"

    validator = DatasetValidator(original_file_path, masked_file_path)

    validator.validate_reconstruction()

    logger.info(f"Validation completed. Warnings saved to {log_file_path}.")
