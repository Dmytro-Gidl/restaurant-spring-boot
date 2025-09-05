# Recommendation Methodology

Our system combines two collaborative filtering approaches:

1. **User-based filtering with mean-centred ratings.**
   - Each user's average rating is subtracted from their reviews to remove bias.
   - Cosine similarity between users is shrunk by the number of co-rated dishes to mitigate small-overlap noise.
   - Predicted scores add the target user's average rating back after aggregating neighbours' deviations.
   - This technique follows standard neighbourhood models described by Herlocker et al. (2004).
2. **Matrix factorisation.**
   - Reviews are converted into a sparse user–dish matrix.
   - Orders without reviews are treated as implicit positive feedback with a rating of 1.
   - An alternating least squares procedure learns latent factors for users and dishes, similar to the method popularised by Koren et al. (2009).
   - Predictions are the dot product of the corresponding factor vectors.

The evaluation harness splits historical reviews into train and test sets and reports precision@k, recall@k and NDCG@k for both algorithms, enabling objective comparison.
