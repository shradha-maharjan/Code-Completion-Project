import torch
from torch.nn import CrossEntropyLoss, MSELoss
import torch.nn.functional as f

from transformers import BartForConditionalGeneration, BartConfig
from transformers.models.bart.modeling_bart import BartClassificationHead, shift_tokens_right
from transformers.modeling_outputs import Seq2SeqLMOutput, Seq2SeqSequenceClassifierOutput

from tqdm import tqdm
import numpy as np
import logging

import enums
from .utils import inputs_to_cuda

logger = logging.getLogger(__name__)

#BartForClassificationAndGeneration inherits from BartForConditionalGeneration, which is a BART (Bidirectional and Auto-Regressive Transformers) model specifically designed for text generation tasks.

class BartForClassificationAndGeneration(BartForConditionalGeneration):

# __init__ method, it sets up the classification head for the model. This includes creating an instance of BartClassificationHead, which will be used for text classification.
    def __init__(self, config: BartConfig, mode=None):
        super(BartForClassificationAndGeneration, self).__init__(config)
        self.mode = None
        if mode:
            self.set_model_mode(mode)

        # classification head
        self.classification_head = BartClassificationHead(
            config.d_model,
            config.d_model,
            config.num_labels,
            config.classifier_dropout,
        )
        self.model._init_weights(self.classification_head.dense)
        self.model._init_weights(self.classification_head.out_proj)

#set_model_mode method is used to switch the mode of the BART model. Modes can be 'gen' (generation), 'cls' (classification), or 'search'.
    def set_model_mode(self, mode):
        assert mode in [enums.MODEL_MODE_GEN, enums.MODEL_MODE_CLS, enums.MODEL_MODE_SEARCH]
        self.mode = mode
        logging.info(f'BART mode switched to {mode}')

#The forward method handles the forward pass of the model. Depending on the mode set (gen or others), it calls the appropriate forward method.
#If the mode is set to 'gen', it calls the forward_gen method.
    def forward(
            self,
            input_ids=None,
            attention_mask=None,
            decoder_input_ids=None,
            decoder_attention_mask=None,
            head_mask=None,
            decoder_head_mask=None,
            cross_attn_head_mask=None,
            encoder_outputs=None,
            past_key_values=None,
            inputs_embeds=None,
            decoder_inputs_embeds=None,
            labels=None,
            use_cache=None,
            output_attentions=None,
            output_hidden_states=None,
            return_dict=None,
            neg_nl_input_ids=None,
            neg_nl_attention_mask=None
    ):
        assert self.mode, 'It is required to specific a mode for BART before the model is passed through'

        if self.mode == enums.MODEL_MODE_GEN:
            # print("Forwarding through mode 'gen'")
            # print("Input IDs:", input_ids)
            # print("Attention Mask:", attention_mask)
            # print("Decoder Input IDs:", decoder_input_ids)
            # print("Decoder Attention Mask:", decoder_attention_mask)
            # print("Labels:", labels)
            return self.forward_gen(input_ids=input_ids,
                                    attention_mask=attention_mask,
                                    decoder_input_ids=decoder_input_ids,
                                    decoder_attention_mask=decoder_attention_mask,
                                    head_mask=head_mask,
                                    decoder_head_mask=decoder_head_mask,
                                    cross_attn_head_mask=cross_attn_head_mask,
                                    encoder_outputs=encoder_outputs,
                                    past_key_values=past_key_values,
                                    inputs_embeds=inputs_embeds,
                                    decoder_inputs_embeds=decoder_inputs_embeds,
                                    labels=labels,
                                    use_cache=use_cache,
                                    output_attentions=output_attentions,
                                    output_hidden_states=output_hidden_states,
                                    return_dict=return_dict)
        else:
            raise ValueError

# This method performs the forward pass for text generation tasks.
# It passes these inputs through the BART model and calculates the logits (scores) for each token in the vocabulary.
# If labels are provided, it calculates the masked language model (MLM) loss based on the predicted logits and the actual labels.
# It returns the outputs, including the loss, logits, and other relevant information.
    def forward_gen(
            self,
            input_ids=None,
            attention_mask=None,
            decoder_input_ids=None,
            decoder_attention_mask=None,
            head_mask=None,
            decoder_head_mask=None,
            cross_attn_head_mask=None,
            encoder_outputs=None,
            past_key_values=None,
            inputs_embeds=None,
            decoder_inputs_embeds=None,
            labels=None,
            use_cache=None,
            output_attentions=None,
            output_hidden_states=None,
            return_dict=None
    ):
        # print("Forwarding through forward_gen")
        # print("Input IDs:", input_ids)
        # print("Attention Mask:", attention_mask)
        # print("Decoder Input IDs:", decoder_input_ids)
        # print("Decoder Attention Mask:", decoder_attention_mask)
        # print("Labels:", labels)
        return_dict = return_dict if return_dict is not None else self.config.use_return_dict

        if labels is not None:
            if decoder_input_ids is None:
                decoder_input_ids = shift_tokens_right(
                    labels, self.config.pad_token_id, self.config.decoder_start_token_id
                )

        outputs = self.model(
            input_ids,
            attention_mask=attention_mask,
            decoder_input_ids=decoder_input_ids,
            encoder_outputs=encoder_outputs,
            decoder_attention_mask=decoder_attention_mask,
            head_mask=head_mask,
            decoder_head_mask=decoder_head_mask,
            cross_attn_head_mask=cross_attn_head_mask,
            past_key_values=past_key_values,
            inputs_embeds=inputs_embeds,
            decoder_inputs_embeds=decoder_inputs_embeds,
            use_cache=use_cache,
            output_attentions=output_attentions,
            output_hidden_states=output_hidden_states,
            return_dict=return_dict,
        )
        
        lm_logits = self.lm_head(outputs[0]) + self.final_logits_bias
        # print("LM Logits Shape:", outputs[0].shape)
        # print("Final Logits Bias Shape:", self.final_logits_bias.shape)

#The masked language model loss is calculated using the CrossEntropyLoss between the predicted logits and the actual labels.
        masked_lm_loss = None
        if labels is not None:
            loss_fct = CrossEntropyLoss()
            masked_lm_loss = loss_fct(lm_logits.view(-1, self.config.vocab_size), labels.view(-1))
            #print("Masked LM Loss:", masked_lm_loss.item())

        if not return_dict:
            output = (lm_logits,) + outputs[1:]
            return ((masked_lm_loss,) + output) if masked_lm_loss is not None else output

#The function returns the masked language model loss and other relevant outputs.
        return Seq2SeqLMOutput(
            loss=masked_lm_loss,
            logits=lm_logits,
            past_key_values=outputs.past_key_values,
            decoder_hidden_states=outputs.decoder_hidden_states,
            decoder_attentions=outputs.decoder_attentions,
            cross_attentions=outputs.cross_attentions,
            encoder_last_hidden_state=outputs.encoder_last_hidden_state,
            encoder_hidden_states=outputs.encoder_hidden_states,
            encoder_attentions=outputs.encoder_attentions,
        )
