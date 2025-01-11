import os

def add_tokens_to_file(input_file, output_file):
    """
    Add <s> and </s> tokens to each line in the input file and save to output file.

    Parameters:
        input_file (str): Path to the input file.
        output_file (str): Path to the output file where modified lines will be saved.
    """
    try:
        # Check if the input file exists
        if not os.path.exists(input_file):
            print(f"Error: Input file '{input_file}' does not exist.")
            return
        
        # Open input and output files
        with open(input_file, 'r', encoding='utf-8') as infile, open(output_file, 'w', encoding='utf-8') as outfile:
            print(f"Processing file: {input_file}")
            
            for line in infile:
                # Skip empty lines
                if line.strip():
                    # Add <s> and </s> tokens
                    updated_line = f"<s> {line.strip()} </s>\n"
                    outfile.write(updated_line)
            
        print(f"Tokens added successfully. Output saved to '{output_file}'.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    # Specify the input and output file paths
    input_file = "/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha/fine-tune/raw_methods_valid.txt"  # Replace with your actual input file path
    output_file = "/home/user1-system11/Documents/research-unixcoder/UniXcoder/dataset/dev.txt"  # Replace with your desired output file path

    # Call the function
    add_tokens_to_file(input_file, output_file)

# import json

# def convert_txt_to_json_single_line(txt_filename, json_filename):
#     # Open the .txt file and read all method snippets
#     with open(txt_filename, 'r', encoding='utf-8') as txt_file:
#         lines = txt_file.readlines()
    
#     json_data = []
    
#     for line in lines:
#         line = line.strip()
#         if not line:
#             continue  # Skip empty lines
        
#         # Split the method into 'input' and 'gt'
#         try:
#             # Find the split point (first `{` is considered the start of the method body)
#             split_index = line.index("{") + 1  # Include the opening brace in 'input'
#             input_snippet = f"<s> {line[:split_index].strip()}"
#             gt_snippet = line[split_index:].strip()
            
#             # Add closing </s> to 'input'
#             #input_snippet += " </s>"
            
#             # Prepare the JSON entry
#             json_entry = {
#                 "input": input_snippet,
#                 "gt": gt_snippet
#             }
#             json_data.append(json_entry)
#         except ValueError:
#             # If no `{` is found, skip this line
#             print(f"Skipping line (no method body found): {line}")
#             continue
    
#     # Write each JSON object on a single line
#     with open(json_filename, 'w', encoding='utf-8') as json_file:
#         for entry in json_data:
#             json_file.write(json.dumps(entry) + "\n")
    
#     print(f"Conversion complete. JSON saved to {json_filename}")

# convert_txt_to_json_single_line("/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/data_shradha/fine-tune/raw_methods_valid.txt", "/home/user1-system11/Documents/research-unixcoder/UniXcoder/dataset/dev.json")
