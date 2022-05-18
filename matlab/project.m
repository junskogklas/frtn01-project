%% Project Kalman design a,adot
clear
clc

h = 5e-3;

Phi_imu = [1 0;h 1];
G_imu = [h;0];
C_imu = eye(2);

R1 = 7e-4;
R2 = [4e-7 0;0 8e-9];

%R1 = 5e-3;
%R2 = [2e-6 0;0 1e-5];

[L_imu,~,~,Eig_imu] = dlqe(Phi_imu,G_imu,C_imu,R1,R2)

%% State Space alpha

Ts = h;
 
imu_kalman = ss( ... % The state-space representation
Phi_imu*(eye(2) - L_imu*C_imu), ... % The "Phi" matrix
Phi_imu*L_imu, ... % The "Gamma" matrix
eye(2) - L_imu*C_imu, ... % The "C" matrix
L_imu, ... % The "D" matrix
Ts);

bw_imu = bandwidth_mimo(imu_kalman)

%% theta,thetadot

Phi_wheel = [1 0;h 1];
G_wheel = [h;0];
C_wheel = [0 1];

R3 = 5e-1;
R4 = 1e-6;

[L_wheel,~,~,Eig_wheel] = dlqe(Phi_wheel,G_wheel,C_wheel,R3,R4)

%% State Space Theta

wheel_kalman = ss( ... % The state-space representation
Phi_wheel*(eye(2) - L_wheel*C_wheel), ... % The "Phi" matrix
Phi_wheel*L_wheel, ... % The "Gamma" matrix
eye(2) - L_wheel*C_wheel, ... % The "C" matrix
L_wheel, ... % The "D" matrix
Ts);

bw_wheel = bandwidth_mimo(wheel_kalman)

%% System model discretization

A = [-3.1 58.4 62.7 0;
     1    0    0    0;
     40.1 -318 -766 0;
     0    0    1    0];
 
 B = [-148; 0; 1808; 0];
 
 C = eye(4);
 
 sys = ss(A,B,C,0);
 
 sysd = c2d(sys, Ts, 'zoh');
 
 P = pole(sysd)
 
 
 %% LQ-design
 
 m1 = 10;
 m2 = 0.5;
 m3 = 100;
 m4 = 82;% 3cm z-distance
 mu = 50;
 
 Q1 = diag([1./(m1.^2) 1./(m2.^2) 1./(m3.^2) 1./(m4.^2)]);
 Q2 = 1./(mu.^2);
 
 feedback_gain = dlqr(sysd.A, sysd.B, Q1, Q2);
 
 P_cl = eig(sysd.A - sysd.B*feedback_gain)
 
 %% Integral action
 
 mi = pi/1.5;                             % Integral state penalty
Q1e = blkdiag(Q1, 1/mi^2);               % Extended Q1 matrix
sysi = ss(sysd.A, sysd.B, [0 0 0 1], 0, Ts);  % Define system with theta as the only output
Ke = lqi(sysi, Q1e, Q2);                  % Calculate extended feedback gain vector
feedback_gain = Ke(1:4);                 % Extract K
integral_gain = Ke(5);                   % Extract ki

%% System model with batteries

g = 9.82; %m/s^2
L = 11.2e-2; %cm
mp = 351e-3;%g
Ip = 0.00616;%kgm^2
mw = 36e-3; %g
I_cmw = 7.46e-6; %kgm^2
rw = 2.1e-2; %cm
R = 4.4; %ohm
kb = 0.495; %Vs/rad
kt = 0.470; %Nm/amp

q1 = L*mp;
q2 = Ip + L^2*mp;
q3 = kb*kt/R;
q4 = - mp - mw - I_cmw/(rw)^2;

Abat = [q3*(q1-q4*rw)/((q1^2 + q2*q4)*rw) g*q1*q4/(q1^2 + q2*q4) q3*(-q1 + q4*rw)/((q1^2 - q2*q4)*rw^2) 0;
     1                      0                                 0 0                                     ;
     g*q1^2/(q1^2 + q2*q4)  -q3*(q2+q1*rw)/((q1^2-q2*q4)*rw)   0 q3*(q2+q1*rw)/((q1^2 + q2*q4)*rw^2)   ;
     0                      0                                 1 0                                     ];
 
Bbat = [kt*(q1-q4*rw)/(R*(q1^2 - q2*q4)*rw) ;
        0                                   ;
        kt*(-q2+q1*rw)/(R*(q1^2 - q2*q4)*rw);
        0                                   ];
    
Cbat = eye(4);