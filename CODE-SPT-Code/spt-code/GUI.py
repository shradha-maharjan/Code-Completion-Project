import tkinter as tk
from tkinter import ttk
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch
import re


class CodeCompletionApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Code Completion with Probabilities")
        self.root.geometry("900x600")

        self.main_container = tk.Frame(root)
        self.main_container.pack(fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(self.main_container, highlightthickness=0)
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.scrollbar = ttk.Scrollbar(self.main_container, orient=tk.VERTICAL, command=self.canvas.yview)
        self.scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self.canvas.configure(yscrollcommand=self.scrollbar.set)
        self.canvas.bind("<Configure>", lambda e: self.canvas.itemconfig(self.canvas_window, width=e.width))

        self.scrollable_frame = tk.Frame(self.canvas)
        self.canvas_window = self.canvas.create_window((0, 0), window=self.scrollable_frame, anchor="nw")

        self.scrollable_frame.bind(
            "<Configure>", lambda e: self.canvas.configure(scrollregion=self.canvas.bbox("all"))
        )

        self.model_name = "shradha01/llm-coding-tasks-model"
        self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
        self.load_model_and_tokenizer()

        self.main_frame = tk.Frame(self.scrollable_frame)
        self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        self.left_frame = tk.Frame(self.main_frame, width=450)
        self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.input_label = tk.Label(self.left_frame, text="Input Code:", font=("Arial", 12))
        self.input_label.pack(anchor="w", pady=(0, 5))

        self.input_text = tk.Text(self.left_frame, height=25, wrap=tk.WORD, font=("Arial", 12))
        self.input_text.pack(fill=tk.BOTH, expand=True, pady=(0, 10))

        self.extract_button = ttk.Button(self.left_frame, text="Extract Masked Methods", command=self.extract_masked_methods)
        self.extract_button.pack(pady=10)

        self.right_frame = tk.Frame(self.main_frame, width=450)
        self.right_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.options_label = tk.Label(self.right_frame, text="Select an Option:", font=("Arial", 12))
        self.options_label.pack(anchor="w", pady=(0, 5))

        self.options_frame = tk.Frame(self.right_frame)
        self.options_frame.pack(anchor="w", fill=tk.X)

        self.selected_option = tk.StringVar(value="")
        self.options = {}
        self.option_buttons = []

        self.output_label = tk.Label(self.right_frame, text="Generated Candidates:", font=("Arial", 12))
        self.output_label.pack(anchor="w", pady=(10, 5))

        self.output_text = tk.Text(self.right_frame, height=15, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
        self.output_text.pack(fill=tk.BOTH, expand=True)

        self.bottom_frame = tk.Frame(self.scrollable_frame)
        self.bottom_frame.pack(fill=tk.X, padx=10, pady=10)

        self.bottom_frame.columnconfigure(0, weight=1)
        self.bottom_frame.columnconfigure(1, weight=2)
        self.bottom_frame.columnconfigure(2, weight=1)

        self.model_info_button = ttk.Button(self.bottom_frame, text="Model Info", command=self.show_model_info)
        self.model_info_button.grid(row=0, column=0, padx=5, pady=5, sticky="ew")

        self.model_name_dropdown = ttk.Combobox(
            self.bottom_frame,
            state="readonly",
            values=["shradha01/llm-coding-tasks-model", "other-model-1"]
        )
        self.model_name_dropdown.set(self.model_name)
        self.model_name_dropdown.grid(row=0, column=1, padx=5, pady=5, sticky="ew")

        self.generate_candidate_button = ttk.Button(self.bottom_frame, text="Generate Candidates", command=self.generate_candidates)
        self.generate_candidate_button.grid(row=0, column=2, padx=5, pady=5, sticky="ew")

    def load_model_and_tokenizer(self):
        try:
            self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
            self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
            print("Model and tokenizer loaded successfully!")
        except Exception as e:
            print(f"Error loading model or tokenizer: {e}")
            self.model = None
            self.tokenizer = None

    def extract_masked_methods(self):
        input_code = self.input_text.get("1.0", tk.END).strip()
        if not input_code:
            self.display_output("Please enter Java code in the input field.")
            return

        method_pattern = r'((public|private|protected|static|final)?\s+\w+(\s*<[^>]*>)?\s+\w+\s*\([^)]*\)\s*\{(?:[^\{\}]*\{[^\{\}]*\}[^\{\}]*)*?[^\{\}]*\[MSK\][^\{\}]*\})'
        matches = re.findall(method_pattern, input_code, re.DOTALL)

        if not matches:
            self.display_output("No masked methods found in the input code.")
            return

        self.options.clear()
        for widget in self.options_frame.winfo_children():
            widget.destroy()  
        self.option_buttons = []

        for idx, (method, _, _) in enumerate(matches, start=1):  
            if "[MSK]" in method: 
                option_name = f"Option {idx}"
                self.options[option_name] = method.strip()
                button = tk.Radiobutton(
                    self.options_frame,
                    text=method.strip(),
                    variable=self.selected_option,
                    value=option_name,
                    font=("Arial", 10),
                    justify=tk.LEFT,
                    wraplength=400
                )
                button.pack(anchor="w", pady=5)
                self.option_buttons.append(button)

        if self.options:
            self.selected_option.set(next(iter(self.options)))

    def generate_candidates(self):
        selected_option_name = self.selected_option.get()
        selected_code = self.options.get(selected_option_name, "")

        if not selected_code:
            self.display_output("Please select a valid option.")
            return

        input_code = self.input_text.get("1.0", tk.END).strip()
        full_code = input_code + "\n\n" + selected_code

        if not self.model or not self.tokenizer:
            self.display_output("Model or tokenizer not loaded.")
            return

        self.display_output("Generating candidates... Please wait.")

        try:
            encoded = self.tokenizer(full_code, return_tensors="pt")
            outputs = self.model.generate(
                encoded["input_ids"],
                max_length=100,
                num_beams=5,
                num_return_sequences=5,
                return_dict_in_generate=True,
                output_scores=True
            )

            decoded_candidates = [
                self.tokenizer.decode(output, skip_special_tokens=True)
                for output in outputs.sequences
            ]

            probs = [
                torch.exp(score).item()
                for score in outputs.sequences_scores
            ]

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

    def show_model_info(self):
        info = f"Model: {self.model_name}\nTokenizer: {self.tokenizer_name}"
        self.display_output(info)


if __name__ == "__main__":
    root = tk.Tk()
    app = CodeCompletionApp(root)
    root.mainloop()


# import tkinter as tk
# from tkinter import ttk
# from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
# import torch
# import re


# class CodeCompletionApp:
#     def __init__(self, root):
#         self.root = root
#         self.root.title("Code Completion with Probabilities")
#         self.root.geometry("900x600")

#         # Load model and tokenizer
#         self.model_name = "shradha01/llm-coding-tasks-model"
#         self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
#         self.load_model_and_tokenizer()

#         # Create main layout frames
#         self.main_frame = tk.Frame(root)
#         self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

#         # Left side: Input section
#         self.left_frame = tk.Frame(self.main_frame, width=450)
#         self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

#         self.input_label = tk.Label(self.left_frame, text="Input Code:", font=("Arial", 12))
#         self.input_label.pack(anchor="w", pady=(0, 5))

#         self.input_text = tk.Text(self.left_frame, height=25, wrap=tk.WORD, font=("Arial", 12))
#         self.input_text.pack(fill=tk.BOTH, expand=True, pady=(0, 10))

#         self.extract_button = ttk.Button(self.left_frame, text="Extract Masked Methods", command=self.extract_masked_methods)
#         self.extract_button.pack(pady=10)

#         # Right side: Options and candidates
#         self.right_frame = tk.Frame(self.main_frame, width=450)
#         self.right_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

#         self.options_label = tk.Label(self.right_frame, text="Select an Option:", font=("Arial", 12))
#         self.options_label.pack(anchor="w", pady=(0, 5))

#         # Dedicated frame for options
#         self.options_frame = tk.Frame(self.right_frame)
#         self.options_frame.pack(anchor="w", fill=tk.X)

#         # Radio buttons for options
#         self.selected_option = tk.StringVar(value="")
#         self.options = {}
#         self.option_buttons = []

#         # # Generate button
#         # self.run_button = ttk.Button(self.right_frame, text="Generate Candidates", command=self.generate_candidates)
#         # self.run_button.pack(pady=10)

#         # Output section
#         self.output_label = tk.Label(self.right_frame, text="Generated Candidates:", font=("Arial", 12))
#         self.output_label.pack(anchor="w", pady=(10, 5))

#         self.output_text = tk.Text(self.right_frame, height=15, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
#         self.output_text.pack(fill=tk.BOTH, expand=True)

#         # Bottom section for model info and actions
#         self.bottom_frame = tk.Frame(root)
#         self.bottom_frame.pack(fill=tk.X, padx=10, pady=10)

#         # Use a grid layout to span the entire width of the GUI
#         self.bottom_frame.columnconfigure(0, weight=1)
#         self.bottom_frame.columnconfigure(1, weight=1)
#         self.bottom_frame.columnconfigure(2, weight=1)

#         # Model Info button
#         self.model_info_button = ttk.Button(self.bottom_frame, text="Model Info", command=self.show_model_info)
#         self.model_info_button.grid(row=0, column=0, padx=5, pady=5, sticky="ew")

#         # Model Name dropdown
#         self.model_name_dropdown = ttk.Combobox(
#             self.bottom_frame, state="readonly", 
#             values=["shradha01/llm-coding-tasks-model", "other-model-1"]
#         )
#         self.model_name_dropdown.set(self.model_name)
#         self.model_name_dropdown.grid(row=0, column=1, padx=5, pady=5, sticky="ew")

#         # Generate Candidate button
#         self.generate_candidate_button = ttk.Button(self.bottom_frame, text="Generate Candidates", command=self.generate_candidates)
#         self.generate_candidate_button.grid(row=0, column=2, padx=5, pady=5, sticky="ew")

#     def load_model_and_tokenizer(self):
#         try:
#             self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
#             self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
#             print("Model and tokenizer loaded successfully!")
#         except Exception as e:
#             print(f"Error loading model or tokenizer: {e}")
#             self.model = None
#             self.tokenizer = None

#     def extract_masked_methods(self):
#         input_code = self.input_text.get("1.0", tk.END).strip()
#         if not input_code:
#             self.display_output("Please enter Java code in the input field.")
#             return

#         # Refined regex pattern to correctly match methods with [MSK]
#         method_pattern = r'((public|private|protected|static|final)?\s+\w+(\s*<[^>]*>)?\s+\w+\s*\([^)]*\)\s*\{(?:[^\{\}]*\{[^\{\}]*\}[^\{\}]*)*?[^\{\}]*\[MSK\][^\{\}]*\})'
#         matches = re.findall(method_pattern, input_code, re.DOTALL)

#         if not matches:
#             self.display_output("No masked methods found in the input code.")
#             return

#         # Filter matches to include only methods with the [MSK] token
#         self.options.clear()
#         for widget in self.options_frame.winfo_children():
#             widget.destroy()  # Clear previous options
#         self.option_buttons = []

#         for idx, (method, _, _) in enumerate(matches, start=1):  # Extract full method from regex match
#             if "[MSK]" in method:  # Explicitly check for [MSK] token
#                 option_name = f"Option {idx}"
#                 self.options[option_name] = method.strip()
#                 button = tk.Radiobutton(
#                     self.options_frame,
#                     text=method.strip(),
#                     variable=self.selected_option,
#                     value=option_name,
#                     font=("Arial", 10),
#                     justify=tk.LEFT,
#                     wraplength=400
#                 )
#                 button.pack(anchor="w", pady=5)
#                 self.option_buttons.append(button)

#         # Set the first option as selected by default
#         if self.options:
#             self.selected_option.set(next(iter(self.options)))

#         # Ensure the "Generate Candidates" button remains visible
#         self.run_button.pack_forget()  # Remove and re-add to maintain its position
#         self.run_button.pack(pady=10)


#     def generate_candidates(self):
#         selected_option_name = self.selected_option.get()
#         selected_code = self.options.get(selected_option_name, "")

#         if not selected_code:
#             self.display_output("Please select a valid option.")
#             return

#         input_code = self.input_text.get("1.0", tk.END).strip()
#         full_code = input_code + "\n\n" + selected_code

#         if not self.model or not self.tokenizer:
#             self.display_output("Model or tokenizer not loaded.")
#             return

#         self.display_output("Generating candidates... Please wait.")

#         try:
#             encoded = self.tokenizer(full_code, return_tensors="pt")
#             outputs = self.model.generate(
#                 encoded["input_ids"],
#                 max_length=100,
#                 num_beams=5,
#                 num_return_sequences=5,
#                 return_dict_in_generate=True,
#                 output_scores=True
#             )

#             decoded_candidates = [
#                 self.tokenizer.decode(output, skip_special_tokens=True)
#                 for output in outputs.sequences
#             ]

#             probs = [
#                 torch.exp(score).item()
#                 for score in outputs.sequences_scores
#             ]

#             result_text = ""
#             for idx, (candidate, prob) in enumerate(zip(decoded_candidates, probs)):
#                 result_text += f"Candidate {idx + 1}:\n{candidate.strip()}\nProbability: {prob:.4f}\n\n"

#             self.display_output(result_text)

#         except Exception as e:
#             self.display_output(f"Error during prediction: {e}")

#     def display_output(self, text):
#         self.output_text.config(state=tk.NORMAL)
#         self.output_text.delete("1.0", tk.END)
#         self.output_text.insert(tk.END, text)
#         self.output_text.config(state=tk.DISABLED)

#     def show_model_info(self):
#         info = f"Model: {self.model_name}\nTokenizer: {self.tokenizer_name}"
#         self.display_output(info)


# if __name__ == "__main__":
#     root = tk.Tk()
#     app = CodeCompletionApp(root)
#     root.mainloop()

# # import tkinter as tk
# # from tkinter import ttk
# # from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
# # import torch
# # import re


# # class CodeCompletionApp:
# #     def __init__(self, root):
# #         self.root = root
# #         self.root.title("Code Completion with Probabilities")
# #         self.root.geometry("900x600")

# #         # Load model and tokenizer
# #         self.model_name = "shradha01/llm-coding-tasks-model"
# #         self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
# #         self.load_model_and_tokenizer()

# #         # Create main layout frames
# #         self.main_frame = tk.Frame(root)
# #         self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

# #         # Left side: Input section
# #         self.left_frame = tk.Frame(self.main_frame, width=450)
# #         self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# #         self.input_label = tk.Label(self.left_frame, text="Input Code:", font=("Arial", 12))
# #         self.input_label.pack(anchor="w", pady=(0, 5))

# #         self.input_text = tk.Text(self.left_frame, height=25, wrap=tk.WORD, font=("Arial", 12))
# #         self.input_text.pack(fill=tk.BOTH, expand=True, pady=(0, 10))

# #         self.extract_button = ttk.Button(self.left_frame, text="Extract Masked Methods", command=self.extract_masked_methods)
# #         self.extract_button.pack(pady=10)

# #         # Right side: Options and candidates
# #         self.right_frame = tk.Frame(self.main_frame, width=450)
# #         self.right_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# #         self.options_label = tk.Label(self.right_frame, text="Select an Option:", font=("Arial", 12))
# #         self.options_label.pack(anchor="w", pady=(0, 5))

# #         # Dedicated frame for options
# #         self.options_frame = tk.Frame(self.right_frame)
# #         self.options_frame.pack(anchor="w", fill=tk.X)

# #         # Radio buttons for options
# #         self.selected_option = tk.StringVar(value="")
# #         self.options = {}
# #         self.option_buttons = []

# #         # Generate button
# #         self.run_button = ttk.Button(self.right_frame, text="Generate Candidates", command=self.generate_candidates)
# #         self.run_button.pack(pady=10)

# #         # Output section
# #         self.output_label = tk.Label(self.right_frame, text="Generated Candidates:", font=("Arial", 12))
# #         self.output_label.pack(anchor="w", pady=(10, 5))

# #         self.output_text = tk.Text(self.right_frame, height=15, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
# #         self.output_text.pack(fill=tk.BOTH, expand=True)

# #     def load_model_and_tokenizer(self):
# #         try:
# #             self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
# #             self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
# #             print("Model and tokenizer loaded successfully!")
# #         except Exception as e:
# #             print(f"Error loading model or tokenizer: {e}")
# #             self.model = None
# #             self.tokenizer = None

# #     def extract_masked_methods(self):
# #         input_code = self.input_text.get("1.0", tk.END).strip()
# #         if not input_code:
# #             self.display_output("Please enter Java code in the input field.")
# #             return

# #         # Improved regex to capture the full method, including multiple lines and [MSK]
# #         method_pattern = r'((public|private|protected|static|final)\s+\w+(\s*<.*?>)?\s+\w+\(.*?\)\s*\{(?:[^{}]*\{[^{}]*\}[^{}]*)*?[^{}]*\[MSK\][^{}]*\})'
# #         matches = re.findall(method_pattern, input_code, re.DOTALL)

# #         if not matches:
# #             self.display_output("No masked methods found in the input code.")
# #             return

# #         # Update options with extracted methods
# #         self.options.clear()
# #         for widget in self.options_frame.winfo_children():
# #             widget.destroy()  # Clear previous options
# #         self.option_buttons = []

# #         for idx, (method, _, _) in enumerate(matches, start=1):  # Extract full method from regex match
# #             option_name = f"Option {idx}"
# #             self.options[option_name] = method.strip()
# #             button = tk.Radiobutton(
# #                 self.options_frame,
# #                 text=method.strip(),
# #                 variable=self.selected_option,
# #                 value=option_name,
# #                 font=("Arial", 10),
# #                 justify=tk.LEFT,
# #                 wraplength=400  # Ensures the code wraps nicely within the frame
# #             )
# #             button.pack(anchor="w", pady=5)
# #             self.option_buttons.append(button)

# #         # Set the first option as selected by default
# #         if self.options:
# #             self.selected_option.set(next(iter(self.options)))

# #     def generate_candidates(self):
# #         selected_option_name = self.selected_option.get()
# #         selected_code = self.options.get(selected_option_name, "")

# #         if not selected_code:
# #             self.display_output("Please select a valid option.")
# #             return

# #         input_code = self.input_text.get("1.0", tk.END).strip()
# #         full_code = input_code + "\n\n" + selected_code

# #         if not self.model or not self.tokenizer:
# #             self.display_output("Model or tokenizer not loaded.")
# #             return

# #         self.display_output("Generating candidates... Please wait.")

# #         try:
# #             # Tokenize input
# #             encoded = self.tokenizer(full_code, return_tensors="pt")

# #             # Generate outputs
# #             outputs = self.model.generate(
# #                 encoded["input_ids"],
# #                 max_length=100,
# #                 num_beams=5,
# #                 num_return_sequences=5,
# #                 return_dict_in_generate=True,
# #                 output_scores=True
# #             )

# #             # Decode generated sequences
# #             decoded_candidates = [
# #                 self.tokenizer.decode(output, skip_special_tokens=True)
# #                 for output in outputs.sequences
# #             ]

# #             # Calculate probabilities
# #             probs = [
# #                 torch.exp(score).item()  # Convert log score to probability
# #                 for score in outputs.sequences_scores
# #             ]

# #             # Format and display results
# #             result_text = ""
# #             for idx, (candidate, prob) in enumerate(zip(decoded_candidates, probs)):
# #                 result_text += f"Candidate {idx + 1}:\n{candidate.strip()}\nProbability: {prob:.4f}\n\n"

# #             self.display_output(result_text)

# #         except Exception as e:
# #             self.display_output(f"Error during prediction: {e}")

# #     def display_output(self, text):
# #         self.output_text.config(state=tk.NORMAL)
# #         self.output_text.delete("1.0", tk.END)
# #         self.output_text.insert(tk.END, text)
# #         self.output_text.config(state=tk.DISABLED)


# # # Initialize and run the application
# # if __name__ == "__main__":
# #     root = tk.Tk()
# #     app = CodeCompletionApp(root)
# #     root.mainloop()

# # # import tkinter as tk
# # # from tkinter import ttk
# # # from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
# # # import torch
# # # import re


# # # class CodeCompletionApp:
# # #     def __init__(self, root):
# # #         self.root = root
# # #         self.root.title("Code Completion with Probabilities")
# # #         self.root.geometry("900x600")

# # #         # Load model and tokenizer
# # #         self.model_name = "shradha01/llm-coding-tasks-model"
# # #         self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
# # #         self.load_model_and_tokenizer()

# # #         # Create main layout frames
# # #         self.main_frame = tk.Frame(root)
# # #         self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

# # #         # Left side: Input section
# # #         self.left_frame = tk.Frame(self.main_frame, width=450)
# # #         self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# # #         self.input_label = tk.Label(self.left_frame, text="Input Code:", font=("Arial", 12))
# # #         self.input_label.pack(anchor="w", pady=(0, 5))

# # #         self.input_text = tk.Text(self.left_frame, height=25, wrap=tk.WORD, font=("Arial", 12))
# # #         self.input_text.pack(fill=tk.BOTH, expand=True, pady=(0, 10))

# # #         self.extract_button = ttk.Button(self.left_frame, text="Extract Masked Methods", command=self.extract_masked_methods)
# # #         self.extract_button.pack(pady=10)

# # #         # Right side: Options and candidates
# # #         self.right_frame = tk.Frame(self.main_frame, width=450)
# # #         self.right_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# # #         self.options_label = tk.Label(self.right_frame, text="Select an Option:", font=("Arial", 12))
# # #         self.options_label.pack(anchor="w", pady=(0, 5))

# # #         # Radio buttons for options
# # #         self.selected_option = tk.StringVar(value="")
# # #         self.options = {}
# # #         self.option_buttons = []

# # #         # Generate button
# # #         self.run_button = ttk.Button(self.right_frame, text="Generate Candidates", command=self.generate_candidates)
# # #         self.run_button.pack(pady=10)

# # #         # Output section
# # #         self.output_label = tk.Label(self.right_frame, text="Generated Candidates:", font=("Arial", 12))
# # #         self.output_label.pack(anchor="w", pady=(10, 5))

# # #         self.output_text = tk.Text(self.right_frame, height=15, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
# # #         self.output_text.pack(fill=tk.BOTH, expand=True)

# # #     def load_model_and_tokenizer(self):
# # #         try:
# # #             self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
# # #             self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
# # #             print("Model and tokenizer loaded successfully!")
# # #         except Exception as e:
# # #             print(f"Error loading model or tokenizer: {e}")
# # #             self.model = None
# # #             self.tokenizer = None

# # #     def extract_masked_methods(self):
# # #         input_code = self.input_text.get("1.0", tk.END).strip()
# # #         if not input_code:
# # #             self.display_output("Please enter Java code in the input field.")
# # #             return

# # #         # Improved regex to capture the full method, including multiple lines and [MSK]
# # #         method_pattern = r'((public|private|protected|static|final)\s+\w+(\s*<.*?>)?\s+\w+\(.*?\)\s*\{(?:[^{}]*\{[^{}]*\}[^{}]*)*?[^{}]*\[MSK\][^{}]*\})'
# # #         matches = re.findall(method_pattern, input_code, re.DOTALL)

# # #         if not matches:
# # #             self.display_output("No masked methods found in the input code.")
# # #             return

# # #         # Update options with extracted methods
# # #         self.options.clear()
# # #         for button in self.option_buttons:
# # #             button.destroy()
# # #         self.option_buttons = []

# # #         for idx, (method, _, _) in enumerate(matches, start=1):  # Extract full method from regex match
# # #             option_name = f"Option {idx}"
# # #             self.options[option_name] = method.strip()
# # #             button = tk.Radiobutton(
# # #                 self.right_frame,
# # #                 text=method.strip(),
# # #                 variable=self.selected_option,
# # #                 value=option_name,
# # #                 font=("Arial", 10),
# # #                 justify=tk.LEFT,
# # #                 wraplength=400  # Ensures the code wraps nicely within the frame
# # #             )
# # #             button.pack(anchor="w", pady=5)
# # #             self.option_buttons.append(button)

# # #         # Set the first option as selected by default
# # #         if self.options:
# # #             self.selected_option.set(next(iter(self.options)))

# # #     def generate_candidates(self):
# # #         selected_option_name = self.selected_option.get()
# # #         selected_code = self.options.get(selected_option_name, "")

# # #         if not selected_code:
# # #             self.display_output("Please select a valid option.")
# # #             return

# # #         input_code = self.input_text.get("1.0", tk.END).strip()
# # #         full_code = input_code + "\n\n" + selected_code

# # #         if not self.model or not self.tokenizer:
# # #             self.display_output("Model or tokenizer not loaded.")
# # #             return

# # #         self.display_output("Generating candidates... Please wait.")

# # #         try:
# # #             # Tokenize input
# # #             encoded = self.tokenizer(full_code, return_tensors="pt")

# # #             # Generate outputs
# # #             outputs = self.model.generate(
# # #                 encoded["input_ids"],
# # #                 max_length=100,
# # #                 num_beams=5,
# # #                 num_return_sequences=5,
# # #                 return_dict_in_generate=True,
# # #                 output_scores=True
# # #             )

# # #             # Decode generated sequences
# # #             decoded_candidates = [
# # #                 self.tokenizer.decode(output, skip_special_tokens=True)
# # #                 for output in outputs.sequences
# # #             ]

# # #             # Calculate probabilities
# # #             probs = [
# # #                 torch.exp(score).item()  # Convert log score to probability
# # #                 for score in outputs.sequences_scores
# # #             ]

# # #             # Format and display results
# # #             result_text = ""
# # #             for idx, (candidate, prob) in enumerate(zip(decoded_candidates, probs)):
# # #                 result_text += f"Candidate {idx + 1}:\n{candidate.strip()}\nProbability: {prob:.4f}\n\n"

# # #             self.display_output(result_text)

# # #         except Exception as e:
# # #             self.display_output(f"Error during prediction: {e}")

# # #     def display_output(self, text):
# # #         self.output_text.config(state=tk.NORMAL)
# # #         self.output_text.delete("1.0", tk.END)
# # #         self.output_text.insert(tk.END, text)
# # #         self.output_text.config(state=tk.DISABLED)


# # # # Initialize and run the application
# # # if __name__ == "__main__":
# # #     root = tk.Tk()
# # #     app = CodeCompletionApp(root)
# # #     root.mainloop()

# # # # import tkinter as tk
# # # # from tkinter import ttk
# # # # from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
# # # # import torch


# # # # class CodeCompletionApp:
# # # #     def __init__(self, root):
# # # #         self.root = root
# # # #         self.root.title("Code Completion with Probabilities")
# # # #         self.root.geometry("900x600")

# # # #         # Load model and tokenizer
# # # #         self.model_name = "shradha01/llm-coding-tasks-model"
# # # #         self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
# # # #         self.load_model_and_tokenizer()

# # # #         # Create main layout frames
# # # #         self.main_frame = tk.Frame(root)
# # # #         self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

# # # #         # Left side: Input section
# # # #         self.left_frame = tk.Frame(self.main_frame, width=450)
# # # #         self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# # # #         self.input_label = tk.Label(self.left_frame, text="Input Code:", font=("Arial", 12))
# # # #         self.input_label.pack(anchor="w", pady=(0, 5))

# # # #         self.input_text = tk.Text(self.left_frame, height=25, wrap=tk.WORD, font=("Arial", 12))
# # # #         self.input_text.pack(fill=tk.BOTH, expand=True, pady=(0, 10))

# # # #         # Right side: Options and candidates
# # # #         self.right_frame = tk.Frame(self.main_frame, width=450)
# # # #         self.right_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

# # # #         self.options_label = tk.Label(self.right_frame, text="Select an Option:", font=("Arial", 12))
# # # #         self.options_label.pack(anchor="w", pady=(0, 5))

# # # #         # Radio buttons for options
# # # #         self.selected_option = tk.StringVar(value="Option 1")
# # # #         self.options = {
# # # #             "Option 1": "@Override public int hashCode() {\n    final long v = currentValue.get();\n    return (int) v ^ (int)([MSK]);\n}",
# # # #             "Option 2": "public void writeLock() {\n    this.fsLock.longReadLock().lock();\n    [MSK].lock();\n}"
# # # #         }
# # # #         for option_name, option_code in self.options.items():
# # # #             tk.Radiobutton(
# # # #                 self.right_frame,
# # # #                 text=option_code,
# # # #                 variable=self.selected_option,
# # # #                 value=option_name,
# # # #                 font=("Arial", 10),
# # # #                 justify=tk.LEFT,
# # # #                 wraplength=400  # Ensures the code wraps nicely within the frame
# # # #             ).pack(anchor="w", pady=5)

# # # #         # Generate button
# # # #         self.run_button = ttk.Button(self.right_frame, text="Generate Candidates", command=self.generate_candidates)
# # # #         self.run_button.pack(pady=10)

# # # #         # Output section
# # # #         self.output_label = tk.Label(self.right_frame, text="Generated Candidates:", font=("Arial", 12))
# # # #         self.output_label.pack(anchor="w", pady=(10, 5))

# # # #         self.output_text = tk.Text(self.right_frame, height=15, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
# # # #         self.output_text.pack(fill=tk.BOTH, expand=True)

# # # #     def load_model_and_tokenizer(self):
# # # #         try:
# # # #             self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
# # # #             self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
# # # #             print("Model and tokenizer loaded successfully!")
# # # #         except Exception as e:
# # # #             print(f"Error loading model or tokenizer: {e}")
# # # #             self.model = None
# # # #             self.tokenizer = None

# # # #     def generate_candidates(self):
# # # #         # Get the selected option code
# # # #         selected_option_name = self.selected_option.get()
# # # #         selected_code = self.options.get(selected_option_name, "")
        
# # # #         if not selected_code:
# # # #             self.display_output("Please select a valid option.")
# # # #             return
        
# # # #         # Combine input code and selected option code
# # # #         input_code = self.input_text.get("1.0", tk.END).strip()
# # # #         full_code = input_code + "\n\n" + selected_code

# # # #         if not self.model or not self.tokenizer:
# # # #             self.display_output("Model or tokenizer not loaded.")
# # # #             return

# # # #         self.display_output("Generating candidates... Please wait.")

# # # #         try:
# # # #             # Tokenize input
# # # #             encoded = self.tokenizer(full_code, return_tensors="pt")

# # # #             # Generate outputs
# # # #             outputs = self.model.generate(
# # # #                 encoded["input_ids"],
# # # #                 max_length=100,
# # # #                 num_beams=5,
# # # #                 num_return_sequences=5,
# # # #                 return_dict_in_generate=True,
# # # #                 output_scores=True
# # # #             )

# # # #             # Decode generated sequences
# # # #             decoded_candidates = [
# # # #                 self.tokenizer.decode(output, skip_special_tokens=True)
# # # #                 for output in outputs.sequences
# # # #             ]

# # # #             # Calculate probabilities
# # # #             probs = [
# # # #                 torch.exp(score).item()  # Convert log score to probability
# # # #                 for score in outputs.sequences_scores
# # # #             ]

# # # #             # Format and display results
# # # #             result_text = ""
# # # #             for idx, (candidate, prob) in enumerate(zip(decoded_candidates, probs)):
# # # #                 result_text += f"Candidate {idx + 1}:\n{candidate.strip()}\nProbability: {prob:.4f}\n\n"

# # # #             self.display_output(result_text)

# # # #         except Exception as e:
# # # #             self.display_output(f"Error during prediction: {e}")

# # # #     def display_output(self, text):
# # # #         self.output_text.config(state=tk.NORMAL)
# # # #         self.output_text.delete("1.0", tk.END)
# # # #         self.output_text.insert(tk.END, text)
# # # #         self.output_text.config(state=tk.DISABLED)


# # # # # Initialize and run the application
# # # # if __name__ == "__main__":
# # # #     root = tk.Tk()
# # # #     app = CodeCompletionApp(root)
# # # #     root.mainloop()
