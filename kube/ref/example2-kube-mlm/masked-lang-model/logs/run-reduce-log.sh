#!/bin/bash

# Check if a file is provided as argument
if [ $# -ne 2 ]; then
    echo "Usage: $0 <input_file> <output_file>"
    exit 1
fi

input_file="$1"
output_file="$2"

# Check if the input file exists
if [ ! -f "$input_file" ]; then
    echo "Error: Input file '$input_file' not found."
    exit 1
fi

# Function to process each line and reduce the progress bar display
process_lines() {
    # Read each line from the input file
    while IFS= read -r line; do
        # Extract progress percentage, data transfer, and speed from the line
        progress=$(echo "$line" | awk '{print $2}')
        data_transfer=$(echo "$line" | awk '{print $8}')
        speed=$(echo "$line" | awk '{print $13}')

        # Reduce the length of progress bar to first few characters and last few characters
        progress_trimmed="${progress:0:5}...${progress: -4}"

        # Format the reduced progress bar with data transfer and speed
        processed_line=$(printf "Downloading data: %s %s [%s]\n" "$progress_trimmed" "$data_transfer" "$speed")

        # Append both original and processed lines to the output file
        echo -e "$line\n$processed_line" >> "$output_file"
    done < "$input_file"
}

# Call the function to process and save the lines to the output file
process_lines

echo "Output saved to $output_file"
