%%%%
%Discover a set of residuals for all days of the week.  Residuals are
%extracted and then clustered.
%%%%
clear all
load './data/simulatedData.mat'

ar = 1;
diff = 1;
ma = 1;
sar = 0;
sdiff = blocksInDay;
sma = 1;

allWindows = [];
allDayNums = [];

arimaModel = arima('ARLags', 1:ar, 'D', diff, 'MALags', 1:ma, ...
            'SARLags', 1:sar, 'Seasonality', sdiff, 'SMALags', 1:sma);

model = estimate(arimaModel, data, 'print', false);
res = infer(model, data);
fitdist(res, 'normal')

%Plot activities
for i = 1:size(actTimes, 2)
    plot(res(actTimes(i):actTimes(i) + 9));
    hold on
end

save('./data/simulatedRun.mat', 'data', 'times', 'actTimes', 'blocksInDay', 'model', 'res');