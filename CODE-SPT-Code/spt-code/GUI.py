import tkinter as tk
from tkinter import ttk
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, pipeline
import torch

import os

# if os.environ.get('DISPLAY','') == '':
#     print('no display found. Using :0.0')
#     os.environ.__setitem__('DISPLAY', ':0.0')

class CodeCompletionApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Code Completion with Probabilities")
        self.root.geometry("700x500")
        
        # Load model and tokenizer
        self.model_name = "shradha01/llm-coding-tasks-model"
        self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
        self.load_model_and_tokenizer()
        
        # Input section
        self.input_label = tk.Label(root, text="Enter Incomplete Code:", font=("Arial", 12))
        self.input_label.pack(anchor="w", padx=10, pady=(10, 0))
        
        self.input_text = tk.Text(root, height=8, wrap=tk.WORD, font=("Arial", 12))
        self.input_text.pack(fill=tk.BOTH, expand=True, padx=10, pady=(0, 10))
        
        # Output section
        self.output_label = tk.Label(root, text="Generated Candidates:", font=("Arial", 12))
        self.output_label.pack(anchor="w", padx=10, pady=(10, 0))
        
        self.output_text = tk.Text(root, height=12, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
        self.output_text.pack(fill=tk.BOTH, expand=True, padx=10, pady=(0, 10))
        
        # Summarize Button
        self.run_button = ttk.Button(root, text="Generate Candidates", command=self.generate_candidates)
        self.run_button.pack(pady=10)
    
    def load_model_and_tokenizer(self):
        try:
            self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
            self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
            print("Model and tokenizer loaded successfully!")
        except Exception as e:
            print(f"Error loading model or tokenizer: {e}")
            self.model = None
            self.tokenizer = None
    
    def generate_candidates(self):
        incomplete_code = self.input_text.get("1.0", tk.END).strip()
        if not incomplete_code:
            self.display_output("Please enter some code to complete.")
            return
        
        if not self.model or not self.tokenizer:
            self.display_output("Model or tokenizer not loaded.")
            return
        
        self.display_output("Generating candidates... Please wait.")
        
        try:
            # Tokenize input
            encoded = self.tokenizer(incomplete_code, return_tensors="pt")
            
            # Generate outputs
            outputs = self.model.generate(
                encoded["input_ids"],
                max_length=50,
                num_beams=5,
                num_return_sequences=5,
                return_dict_in_generate=True,
                output_scores=True
            )
            
            # Decode generated sequences
            decoded_candidates = [
                self.tokenizer.decode(output, skip_special_tokens=True)
                for output in outputs.sequences
            ]
            
            # Calculate probabilities
            probs = [
                torch.exp(score).item()  # Convert log score to probability
                for score in outputs.sequences_scores
            ]
            
            # Format and display results
            result_text = ""
            for idx, (candidate, prob) in enumerate(zip(decoded_candidates, probs)):
                result_text += f"Candidate {idx + 1}:\n{candidate.strip()}\nProbability: {prob:.4f}\n\n"
            
            self.display_output(result_text)
        
        except Exception as e:
            self.display_output(f"Error during prediction: {e}")
    
    def display_output(self, text):
        self.output_text.config(state=tk.NORMAL)
        self.output_text.delete("1.0", tk.END)
        self.output_text.insert(tk.END, text)
        self.output_text.config(state=tk.DISABLED)

# Initialize and run the application
if __name__ == "__main__":
    root = tk.Tk()
    app = CodeCompletionApp(root)
    root.mainloop()
