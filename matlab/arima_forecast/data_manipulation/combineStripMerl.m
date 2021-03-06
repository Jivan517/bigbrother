%Combine merl data and strip it.
clear all;

sensorNumber = 59;
stripDay = 4;

load('./data/merlData.mat');

dataCombined = data.data(sensorNumber, :);
timesCombined = data.times;

dayOfWeek = weekday(timesCombined);

%Extract a day

tmp = (dayOfWeek == stripDay);
dataCombined = dataCombined(tmp);
timesCombined = timesCombined(tmp);

% 
% [means, stds] = dailyMean(dataCombined, timesCombined, data.blocksInDay, 'smooth', true);
% plotMean(means(stripDay, :), 'std', stds(stripDay, :));

newSize = floor(size(dataCombined, 2)/data.blocksInDay);
newData = dataCombined(:, 1:newSize*data.blocksInDay);
newTimes = timesCombined(:, 1:newSize*data.blocksInDay);

dataCombined = reshape(newData, data.blocksInDay, newSize)';
timesCombined = reshape(newTimes, data.blocksInDay, newSize)';

%Make a matrix of the sum of all days
daySums = sum(dataCombined, 2)';
% 
% %Remove zero days
% nonZeroDays = daySums > 0;
% 
% stripData = stripData(nonZeroDays, :);
% stripTimes = stripTimes(nonZeroDays, :);

%Concate the data back to one dataset
sd = reshape(dataCombined', 1, size(dataCombined, 1)*size(dataCombined, 2));
st = reshape(timesCombined', 1, size(timesCombined, 1)*size(timesCombined, 2));

sd = smooth(sd, 3)';

[means, stds] = dailyMean(sd, st, data.blocksInDay, 'smooth', false);
plotMean(means(stripDay, :), 'std', stds(stripDay, :));


%Remove the top n% of outliers and renormalize
removePercent = 0.001;
nRemove = floor(removePercent * size(sd, 2));

[tmp, ind] = sort(sd, 'descend');
sd(ind(1, 1:nRemove)) = tmp(1, ind(1, nRemove + 1));
sd(ind(1, end-nRemove:end)) = tmp(1, ind(1, end - nRemove - 1));

%Normalize
sd = 2*(sd - min(sd))/(max(sd) - min(sd)) - 1;

data.data = sd;
data.times = st;
data.startTime = 0;
data.endTime = 0;
data.dayOfWeek = weekday(st);
data.sensor = sensorNumber;
data.stripDays = [stripDay];

save('./data/merlDataClean.mat', 'data');