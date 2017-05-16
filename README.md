# Greedo Layout for Android
A drop-in custom layout manager for Android RecyclerViews to layout a grid of photos while respecting their aspect ratios.

![image](screenshot.png)

## Setup
Download the latest release via Gradle:
``` groovy
repositories {
	maven { url 'https://github.com/500px/greedo-layout-for-android/raw/master/releases/' }
}

dependencies {
	compile 'com.fivehundredpx:greedo-layout:1.1.0'
}
```

## Usage
See the sample project for a complete solution on how to use GreedoLayout. Below are the specific steps.

```java
// Create an instance of the GreedoLayoutManager and pass it to the RecyclerView
MyRecyclerAdapter recyclerAdapter = new MyRecyclerAdapter(this);
GreedoLayoutManager layoutManager = new GreedoLayoutManager(recyclerAdapter);

RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
recyclerView.setLayoutManager(layoutManager);
recyclerView.setAdapter(recyclerAdapter);

// Set the max row height in pixels
layoutManager.setMaxRowHeight(300);

// If you would like to add spacing between items (Note, MeasUtils is in the sample project)
int spacing = MeasUtils.dpToPx(4, this);
recyclerView.addItemDecoration(new GreedoSpacingItemDecoration(spacing));
```

And then, in your RecyclerView adapter, or some other class of your choosing, implement `SizeCalculatorDelegate`. This implementation got passed to the layout manager above.
```java
public class MyRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> implements SizeCalculatorDelegate {
    @Override
    public double aspectRatioForIndex(int index) {
    	// Return the aspect ratio of your image at the given index
    }
}
```

## License
GreedoLayout is released under the MIT license. See LICENSE for details.
