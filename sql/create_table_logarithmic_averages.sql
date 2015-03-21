CREATE TABLE logarithmic_averages (
 t bigint(16),
 w_avg decimal(16,10),
 h_avg decimal(16,10),
 L int(12),
 x decimal(5,4),
 p_diff decimal(5,4),
 l_0 decimal(7,4),
 S int(4),
 A decimal(7,4),
 PRIMARY KEY(t, L, x, p_diff, l_0)
);