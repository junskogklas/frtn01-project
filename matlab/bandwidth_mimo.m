function bw_mat = bandwidth_mimo(sys)
%BANDWIDTH_MIMO Calculates the bandwidth of a MIMO system
%   Function which takes a system 'sys' and returns the bandwidth for the
%   internal signal paths.
%
%       bw_mimo = bandwidth_mimo(state_space_system)
% 
% RETURNS:
%   bw_mat = a matrix of size (nbr_of_outputs x nbr_of_inputs)
%   corresponding to the bandwidth of the transfer function between input
%   and output

[nbr_output, nbr_input] = size(sys);
bw_mat = zeros(nbr_output, nbr_input);

for out = 1:nbr_output
    for in = 1:nbr_input
        bw_mat(out, in) = bandwidth(sys(out, in));
    end
end
end

