function [ windows, indexes ] = largestWindow(data, windowSize, numWindows)
    %Finds the windows with the largest total value for a given size
    %window.  Relies on peak finding algorithm to work.

    ndata = smooth(data, 'lowess');
    ndata = ndata';
    [pks, locs] = findpeaks(abs(ndata));
    [~, ind] = sort(pks, 'descend');
    windows = cell(1, numWindows);
    indexes = zeros(1, numWindows);
    
    for i = 1:numWindows
        win = floor(windowSize / 2);
        windows{1, i} = ndata(:, locs(ind(i)) - win : locs(ind(i)) + win);
        indexes(1, i) = locs(ind(i));
    end
end

