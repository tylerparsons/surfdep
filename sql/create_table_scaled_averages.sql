CREATE TABLE scaled_averages (
 h_avg int(12),
 w_avg decimal(16,10),
 L int(12),
 x decimal(5,4),
 p_diff decimal(5,4),
 l_0 decimal(7,4),
 S int(4),
 PRIMARY KEY(h_avg, L, x, p_diff, l_0)
);